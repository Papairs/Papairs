package com.papairs.docs.service;

import com.papairs.docs.exception.InvalidRequestException;
import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.PageMember;
import com.papairs.docs.model.enums.MemberRole;
import com.papairs.docs.repository.PageMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PageMemberService {
    private final PageMemberRepository pageMemberRepository;
    private final PermissionService permissionService;
    private final UserService userService;

    public PageMemberService(PageMemberRepository pageMemberRepository, PermissionService permissionService, UserService userService) {
        this.pageMemberRepository = pageMemberRepository;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    /**
     * Adds a user as a member to a specific page
     * This action is restricted to the page owner or members with the {@code EDITOR} role
     * @param pageId The ID of the page where the member will be added
     * @param requestingUserId The ID of the user performing this action
     * @param targetUserId The ID of the user to be added as a member
     * @param role The {@link MemberRole} to assign to the new member
     * @return The newly created {@link PageMember} entity
     * @throws InvalidRequestException if the target user is the page owner or is already a member
     */
    @Transactional
    public PageMember addMember(
            String pageId,
            String requestingUserId,
            String targetUserId,
            MemberRole role
    ) {
        permissionService.requireMemberManagement(pageId, requestingUserId);

        if (!userService.userExists(targetUserId)) {
            throw new ResourceNotFoundException("Target user does not exist");
        }

        if (permissionService.isOwner(pageId, targetUserId)) {
            throw new InvalidRequestException("Cannot add owner as member");
        }

        if (pageMemberRepository.existsByPageIdAndUserId(pageId, targetUserId)) {
            throw new InvalidRequestException("User is already a member");
        }

        PageMember member = new PageMember(pageId, targetUserId, role);
        return pageMemberRepository.save(member);
    }

    /**
     * Updates the role of an existing page member
     * Only the page owner or an {@code EDITOR} can change a member's role
     * @param pageId The ID of the page
     * @param requestingUserId The ID of the user making the request
     * @param targetUserId The ID of the member whose role is to be updated
     * @param newRole The new {@link MemberRole} to assign
     * @return The updated {@link PageMember} entity
     * @throws ResourceNotFoundException if the target user is not a member of the page
     */
    @Transactional
    public PageMember updateMemberRole(
            String pageId,
            String requestingUserId,
            String targetUserId,
            MemberRole newRole
    ) {
        permissionService.requireMemberManagement(pageId, requestingUserId);

        if (!userService.userExists(targetUserId)) {
            throw new ResourceNotFoundException("Target user does not exist");
        }

        PageMember member = pageMemberRepository.findByPageIdAndUserId(pageId, targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        member.setRole(newRole);
        return pageMemberRepository.save(member);
    }

    /**
     * Removes a member from a page
     * This action is restricted to the page owner or members with the {@code EDITOR} role
     * @param pageId The ID of the page from which to remove the member
     * @param requestingUserId The ID of the user performing the removal
     * @param targetUserId The ID of the member to remove
     * @throws ResourceNotFoundException if the target user is not a member of the page
     */
    @Transactional
    public void removeMember(
            String pageId,
            String requestingUserId,
            String targetUserId
    ) {
        permissionService.requireMemberManagement(pageId, requestingUserId);

        if (!userService.userExists(targetUserId)) {
            throw new ResourceNotFoundException("Target user does not exist");
        }

        PageMember memberToRemove = pageMemberRepository.findByPageIdAndUserId(pageId, targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        pageMemberRepository.delete(memberToRemove);
    }

    /**
     * Retrieves a list of all members associated with a page
     * The requesting user must have at least view access to the page
     * @param pageId The ID of the page.
     * @param requestingUserId The ID of the user requesting the member list
     * @return A {@link List} of {@link PageMember} entities
     */
    public List<PageMember> getPageMembers(String pageId, String requestingUserId) {
        permissionService.requirePageAccess(pageId, requestingUserId);
        return pageMemberRepository.findByPageId(pageId);
    }

    /**
     * Retrieves a list of page IDs for all pages a user is a member of
     * @param userId The ID of the user
     * @return A {@link List} of page IDs
     */
    public List<String> getUserPageMemberships(String userId) {
        return pageMemberRepository.findByUserId(userId).stream()
            .map(PageMember::getPageId)
            .collect(Collectors.toList());
    }

    /**
     * Checks if a user is a member of a specific page
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the user is a member, {@code false} otherwise
     */
    public boolean isMember(String pageId, String userId) {
        return pageMemberRepository.existsByPageIdAndUserId(pageId, userId);
    }

    /**
     * Allows a user to leave a page, effectively removing their own membership
     * The page owner cannot use this method and must delete the page
     * TODO: Subject to change
     * @param pageId The ID of the page to leave
     * @param userId The ID of the user leaving the page
     * @throws InvalidRequestException if the user is the page owner or is not a member
     */
    @Transactional
    public void leavePage(String pageId, String userId) {
        permissionService.requirePageAccess(pageId, userId);

        if (permissionService.isOwner(pageId, userId)) {
            throw new InvalidRequestException("Cannot leave a page you own. Transfer ownership or delete the page");
        }

        if (!pageMemberRepository.existsByPageIdAndUserId(pageId, userId)) {
            throw new InvalidRequestException("You are not a member of this page");
        }

        pageMemberRepository.deleteByPageIdAndUserId(pageId, userId);
    }
}
