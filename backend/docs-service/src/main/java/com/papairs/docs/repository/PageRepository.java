package com.papairs.docs.repository;

import com.papairs.docs.model.FolderPageCount;
import com.papairs.docs.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, String> {

    /**
     * Finds all pages owned by a specific user
     * @param ownerId The ID of the owner
     * @return A {@link List} of {@link Page} entities owned by the user
     */
    List<Page> findByOwnerId(String ownerId);

    List<Page> findByFolderIdIn(List<String> folderIds);

    /**
     * Finds all pages located within a specific folder
     * @param folderId The ID of the folder
     * @return A {@link List} of {@link Page} entities within the folder.
     */
    List<Page> findByFolderId(String folderId);

    /**
     * Checks if any pages exist within a specific folder.
     * This is more performant than fetching the list of pages and checking if it's empty
     * @param folderId The ID of the folder
     * @return {@code true} if at least one page exists, {@code false} otherwise
     */
    boolean existsByFolderId(String folderId);

    /**
     * Counts the number of pages for multiple folders in a single query
     * This method uses a JPQL constructor expression to map the results of the
     * GROUP BY query directly into a {@link FolderPageCount} model
     * @param folderIds A {@link List} of folder IDs for which to count pages
     * @return A {@link List} of {@link FolderPageCount} DTOs, each containing a folderId and its
     * corresponding page count. Folders with zero pages will not be included in the result list
     */
    @Query("SELECT new com.papairs.docs.model.FolderPageCount(p.folderId, COUNT(p)) " +
            "FROM Page p WHERE p.folderId IN :folderIds GROUP BY p.folderId")
    List<FolderPageCount> countPagesInFolders(List<String> folderIds);

    /**
     * Finds all pages a user has access to, either as the owner or as a member
     * Results are sorted numerically by {@code updatedAt}
     * @param userId The ID of the user.
     * @return A {@link List} of distinct {@link Page} entities.
     */
    @Query("SELECT DISTINCT p FROM Page p " +
            "LEFT JOIN PageMember pm ON p.pageId = pm.pageId " +
            "WHERE p.ownerId = :userId OR pm.userId = :userId " +
            "ORDER BY p.updatedAt DESC")
    List<Page> findAllAccessibleByUserId(String userId);

    /**
     * Finds all pages a user has access to along with their role in a single optimized query
     * Returns Object[] with [Page, MemberRole] where MemberRole is null for owned pages
     * @param userId The ID of the user
     * @return A {@link List} of Object arrays containing Page and MemberRole
     */
    @Query("SELECT p, pm.role FROM Page p " +
            "LEFT JOIN PageMember pm ON p.pageId = pm.pageId AND pm.userId = :userId " +
            "WHERE p.ownerId = :userId OR pm.userId = :userId " +
            "ORDER BY p.updatedAt DESC")
    List<Object[]> findAllAccessibleByUserIdWithRole(String userId);

    /**
     * Deletes all pages within a given list of folder IDs in a single bulk operation
     * The {@code clearAutomatically = true} option ensures the persistence context is cleared
     * after the query which preventing issues with detached entities.
     * @param folderIds A {@link List} of folder IDs to delete pages from.
     * @see Modifying#clearAutomatically()
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Page p WHERE p.folderId IN :folderIds")
    void deleteAllByFolderIdIn(List<String> folderIds);
}
