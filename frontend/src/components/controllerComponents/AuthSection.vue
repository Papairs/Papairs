<template>
  <div class="space-y-6">
    <h2 class="text-2xl font-bold text-content-primary dark:text-content-inverse mb-4">
      Authentication Service Tests
    </h2>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Health Check -->
      <TestCard
        title="Health Check"
        description="GET /actuator/health/services/authService"
        button-text="Test Health"
        :is-loading="testManager.isTestLoading('auth-health')"
        :result="formatResult('auth-health')"
        @test="testAuthHealth"
        @clear-result="clearResult('auth-health')"
      />

      <!-- Login -->
      <TestCard
        title="User Login"
        description="POST /api/auth/login"
        button-text="Test Login"
        :form-fields="loginFormFields"
        :is-loading="testManager.isTestLoading('auth-login')"
        :result="formatResult('auth-login')"
        @test="testLogin"
        @clear-result="clearResult('auth-login')"
        @field-update="updateFormField('login', $event)"
      />

      <!-- Register -->
      <TestCard
        title="User Registration"
        description="POST /api/auth/register"
        button-text="Test Register"
        :form-fields="registerFormFields"
        :is-loading="testManager.isTestLoading('auth-register')"
        :result="formatResult('auth-register')"
        @test="testRegister"
        @clear-result="clearResult('auth-register')"
        @field-update="updateFormField('register', $event)"
      />

      <!-- Logout -->
      <TestCard
        title="User Logout"
        description="POST /api/auth/logout"
        button-text="Test Logout"
        :is-loading="testManager.isTestLoading('auth-logout')"
        :result="formatResult('auth-logout')"
        @test="testLogout"
        @clear-result="clearResult('auth-logout')"
      />

      <!-- Validate Token -->
      <TestCard
        title="Validate Token"
        description="POST /api/auth/validate"
        button-text="Test Validate"
        :is-loading="testManager.isTestLoading('auth-validate')"
        :result="formatResult('auth-validate')"
        @test="testValidateToken"
        @clear-result="clearResult('auth-validate')"
      />

      <!-- Change Password -->
      <TestCard
        title="Change Password"
        description="POST /api/auth/change-password"
        button-text="Test Change Password"
        :form-fields="changePasswordFormFields"
        :is-loading="testManager.isTestLoading('auth-change-password')"
        :result="formatResult('auth-change-password')"
        @test="testChangePassword"
        @clear-result="clearResult('auth-change-password')"
        @field-update="updateFormField('changePassword', $event)"
      />
    </div>
  </div>
</template>

<script>
import TestCard from './TestCard.vue';
import { authController } from '@/controllers/index.js';

export default {
  name: 'AuthSection',
  components: {
    TestCard
  },
  props: {
    testManager: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      testForms: {
        login: { email: 'test@example.com', password: 'password123' },
        register: { email: 'newuser@example.com', password: 'newpass123' },
        changePassword: { currentPassword: 'oldpass', newPassword: 'newpass' }
      }
    }
  },
  computed: {
    loginFormFields() {
      return [
        {
          key: 'email',
          label: 'Email',
          type: 'email',
          value: this.testForms.login.email
        },
        {
          key: 'password',
          label: 'Password',
          type: 'password',
          value: this.testForms.login.password
        }
      ];
    },
    registerFormFields() {
      return [
        {
          key: 'email',
          label: 'Email',
          type: 'email',
          value: this.testForms.register.email
        },
        {
          key: 'password',
          label: 'Password',
          type: 'password',
          value: this.testForms.register.password
        }
      ];
    },
    changePasswordFormFields() {
      return [
        {
          key: 'currentPassword',
          label: 'Current Password',
          type: 'password',
          value: this.testForms.changePassword.currentPassword
        },
        {
          key: 'newPassword',
          label: 'New Password',
          type: 'password',
          value: this.testForms.changePassword.newPassword
        }
      ];
    }
  },
  methods: {
    formatResult(testKey) {
      const result = this.testManager.getResult(testKey);
      return result ? this.testManager.formatResult(result) : null;
    },
    
    clearResult(testKey) {
      this.testManager.clearResult(testKey);
    },

    updateFormField(formName, { key, value }) {
      if (this.testForms[formName]) {
        this.testForms[formName][key] = value;
      }
    },

    async testAuthHealth() {
      await this.testManager.executeTest('auth-health', () => authController.checkHealth());
    },

    async testLogin() {
      await this.testManager.executeTest('auth-login', () => 
        authController.login(this.testForms.login)
      );
    },

    async testRegister() {
      await this.testManager.executeTest('auth-register', () => 
        authController.register(this.testForms.register)
      );
    },

    async testLogout() {
      await this.testManager.executeTest('auth-logout', () => 
        authController.logout()
      );
    },

    async testValidateToken() {
      await this.testManager.executeTest('auth-validate', () => 
        authController.validateToken()
      );
    },

    async testChangePassword() {
      await this.testManager.executeTest('auth-change-password', () => 
        authController.changePassword(this.testForms.changePassword)
      );
    }
  }
}
</script>