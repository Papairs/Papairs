<template>
  <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
    <div class="bg-white dark:bg-gray-800 rounded-lg p-6 w-[500px] max-h-[80vh] overflow-y-auto">
      <h2 class="text-xl font-semibold mb-4 text-gray-900 dark:text-white">Share Document</h2>
      
      <!-- Add New Member Form - Only for Owner/Editor -->
      <form v-if="canManageMembers" @submit.prevent="handleSubmit" class="mb-6">
        <div class="mb-4">
          <label for="userId" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Add User by ID
          </label>
          <input
            v-model="userId"
            type="text"
            id="userId"
            required
            placeholder="Enter user ID"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          />
        </div>
        <div class="mb-4">
          <label for="role" class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Role
          </label>
          <select
            v-model="role"
            id="role"
            class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          >
            <option value="VIEWER">Viewer</option>
            <option value="EDITOR">Editor</option>
          </select>
        </div>
        <button
          type="submit"
          class="w-full px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          Add Member
        </button>
      </form>

      <!-- Read-only message for viewers -->
      <div v-else class="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
        <p class="text-sm text-blue-800 dark:text-blue-300">You have view-only access to this document.</p>
      </div>

      <!-- Existing Members List -->
      <div v-if="members.length > 0" class="border-t border-gray-200 dark:border-gray-700 pt-4">
        <h3 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">Current Members</h3>
        <div class="space-y-2">
          <div 
            v-for="member in members" 
            :key="member.userId"
            class="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg"
          >
            <div class="flex-1">
              <p class="text-sm font-medium text-gray-900 dark:text-white">
                {{ member.userId }}
                <span v-if="member.userId === currentUserId" class="text-xs text-gray-500 dark:text-gray-400">(You)</span>
                <span v-if="member.userId === document.ownerId" class="text-xs text-blue-600 dark:text-blue-400">(Owner)</span>
              </p>
              <p class="text-xs text-gray-500 dark:text-gray-400">{{ member.email || 'No email' }}</p>
            </div>
            
            <!-- Role dropdown or display - hidden for current user -->
            <div v-if="member.userId !== currentUserId" class="flex items-center gap-2">
              <select
                v-if="canManageMembers"
                :value="member.role"
                @change="handleRoleChange(member.userId, $event.target.value)"
                class="px-3 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="VIEWER">Viewer</option>
                <option value="EDITOR">Editor</option>
              </select>
              <span v-else class="px-3 py-1 text-sm text-gray-600 dark:text-gray-400">
                {{ member.role }}
              </span>
              
              <!-- Remove member button - only for owner/editor, not for document owner -->
              <button
                v-if="canManageMembers && member.userId !== document.ownerId"
                @click="handleRemoveMember(member.userId)"
                class="p-1 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                title="Remove member"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <!-- Show current user's role (read-only) -->
            <span v-else class="px-3 py-1 text-sm text-gray-600 dark:text-gray-400">
              {{ member.role }}
            </span>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div v-else-if="loading" class="border-t border-gray-200 dark:border-gray-700 pt-4">
        <p class="text-sm text-gray-500 dark:text-gray-400 text-center">Loading members...</p>
      </div>

      <!-- No Members State -->
      <div v-else class="border-t border-gray-200 dark:border-gray-700 pt-4">
        <p class="text-sm text-gray-500 dark:text-gray-400 text-center">No members yet</p>
      </div>

      <!-- Close Button -->
      <div class="mt-6 flex justify-end">
        <button
          type="button"
          @click="$emit('close')"
          class="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
        >
          Close
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { driveService } from '@/utils/driveService'
import auth from '@/utils/auth'

export default {
  name: 'ShareDocumentModal',
  props: {
    document: {
      type: Object,
      required: true
    }
  },
  emits: ['close', 'shared'],
  setup(props, { emit }) {
    const userId = ref('')
    const role = ref('VIEWER')
    const members = ref([])
    const loading = ref(false)
    const currentUserId = ref(auth.getUserId())

    // Check if current user can manage members (is owner or has editor role)
    const canManageMembers = computed(() => {
      if (currentUserId.value === props.document.ownerId) {
        return true // Owner can always manage
      }
      const currentMember = members.value.find(m => m.userId === currentUserId.value)
      return currentMember?.role === 'EDITOR'
    })

    const loadMembers = async () => {
      loading.value = true
      try {
        members.value = await driveService.getDocumentMembers(props.document.pageId)
      } catch (error) {
        console.error('Failed to load members:', error)
        members.value = []
      } finally {
        loading.value = false
      }
    }

    const handleSubmit = async () => {
      if (!canManageMembers.value) return
      
      try {
        await driveService.shareDocument(props.document.pageId, userId.value, role.value)
        userId.value = ''
        role.value = 'VIEWER'
        // Reload members to show the new member
        await loadMembers()
        emit('shared')
      } catch (error) {
        console.error('Failed to share document:', error)
        // Optionally show an error message
      }
    }

    const handleRoleChange = async (memberId, newRole) => {
      if (!canManageMembers.value) return
      
      try {
        await driveService.updateDocumentMemberRole(props.document.pageId, memberId, newRole)
        // Update local state
        const member = members.value.find(m => m.userId === memberId)
        if (member) {
          member.role = newRole
        }
        
      } catch (error) {
        console.error('Failed to update member role:', error)
        // Optionally show an error message
      }
    }

    const handleRemoveMember = async (memberId) => {
      if (!canManageMembers.value) return
      if (memberId === props.document.ownerId) return // Can't remove owner
      
      if (!confirm('Are you sure you want to remove this member?')) {
        return
      }
      
      try {
        await driveService.removeDocumentMember(props.document.pageId, memberId)
        // Remove from local state
        members.value = members.value.filter(m => m.userId !== memberId)
        // Notify parent to refresh document list
      } catch (error) {
        console.error('Failed to remove member:', error)
        // Optionally show an error message
      }
    }

    onMounted(() => {
      loadMembers()
    })

    return {
      userId,
      role,
      members,
      loading,
      currentUserId,
      canManageMembers,
      handleSubmit,
      handleRoleChange,
      handleRemoveMember
    }
  }
}
</script>
