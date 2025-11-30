package com.papairs.docs.repository;

import com.papairs.docs.model.PageMember;
import com.papairs.docs.model.PageMemberId;
import com.papairs.docs.model.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageMemberRepository extends JpaRepository<PageMember, PageMemberId> {

    /**
     * Finds all members of a specific page
     * @param pageId The ID of the page
     * @return A list of {@link PageMember} entities
     */
    List<PageMember> findByPageId(String pageId);

    /**
     * Finds all pages a specific user is a member of
     * @param userId The ID of the user
     * @return A list of {@link PageMember} entities
     */
    List<PageMember> findByUserId(String userId);

    /**
     * Finds a specific page membership by page and user IDs
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return An {@link Optional} containing the {@link PageMember} if found
     */
    Optional<PageMember> findByPageIdAndUserId(String pageId, String userId);

    /**
     * Checks if a user is a member of a specific page
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @return {@code true} if the membership exists, {@code false} otherwise
     */
    boolean existsByPageIdAndUserId(String pageId, String userId);

    /**
     * Checks if a member exists for a given page and user with a specific role
     * @param pageId The ID of the page
     * @param userId The ID of the user
     * @param role The role to check for
     * @return {@code true} if such a member exists, {@code false} otherwise
     */
    boolean existsByPageIdAndUserIdAndRole(String pageId, String userId, MemberRole role);

    /**
     * Retrieves all page IDs for a given user
     * Useful for efficiently listing all pages a user has access to
     * @param userId The ID of the user
     * @return A list of page IDs
     */
    @Query("SELECT pm.pageId FROM PageMember pm WHERE pm.userId = :userId")
    List<String> findPageIdsByUserId(String userId);

    /**
     * Deletes a specific page membership
     * @param pageId The ID of the page
     * @param userId The ID of the user
     */
    @Modifying
    void deleteByPageIdAndUserId(String pageId, String userId);

    /**
     * Deletes all member memberships for a specific page in a single bulk operation
     * @param pageId The ID of the page whose members should be deleted
     */
    @Modifying
    void deleteAllByPageId(String pageId);

    
}
