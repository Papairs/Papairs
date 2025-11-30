package com.papairs.docs.service;

import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.model.FolderPageCount;
import com.papairs.docs.repository.FolderRepository;
import com.papairs.docs.repository.PageMemberRepository;
import com.papairs.docs.repository.PageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.papairs.docs.model.Page;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContentService {
    private final FolderRepository folderRepository;
    private final PageRepository pageRepository;
    private final PageMemberRepository pageMemberRepository;

    public ContentService(FolderRepository folderRepository, PageRepository pageRepository, PageMemberRepository pageMemberRepository) {
        this.folderRepository = folderRepository;
        this.pageRepository = pageRepository;
        this.pageMemberRepository = pageMemberRepository;
    }

    /**
     * Recursively deletes a folder and all its contents including subfolders and pages
     * @param folderId The ID of the top-level folder to delete
     * @throws ResourceNotFoundException if the specified folder does not exist
     */
    @Transactional
    public void deleteFolderWithContents(String folderId) {
        if (!folderRepository.existsById(folderId)) {
            throw new ResourceNotFoundException("Folder not found");
        }

        List<String> folderIdsToDelete = folderRepository.findAllDescendantIds(folderId);
        folderIdsToDelete.add(folderId);

        pageRepository.deleteAllByFolderIdIn(folderIdsToDelete);

        folderRepository.deleteAllByFolderIdIn(folderIdsToDelete);
    }

    /**
     * Deletes a page and all its associated member permissions
     * @param pageId The ID of the page to delete
     * @throws ResourceNotFoundException if the specified page does not exist
     */
    @Transactional
    public void deletePageWithMembers(String pageId) {
        if (!pageRepository.existsById(pageId)) {
            throw new ResourceNotFoundException("Page not found");
        }

        pageMemberRepository.deleteAllByPageId(pageId);
        pageRepository.deleteById(pageId);
    }

    /**
     * Checks if a folder contains any content (either subfolders or pages)
     * @param folderId The ID of the folder to check
     * @return {@code true} if the folder has any children, {@code false} otherwise
     */
    public boolean folderHasContent(String folderId) {
        return folderRepository.existsByParentFolderId(folderId) || pageRepository.existsByFolderId(folderId);
    }

    /**
     * Retrieves the count of pages for each folder in a given list
     * @param folderIds A {@link List} of folder IDs
     * @return A {@link Map} where the key is the folder ID and the value is the count of pages
     *         Folders from the input list that have no pages will be included with a count of 0
     */
    public Map<String, Long> getPageCountsForFolders(List<String> folderIds) {
        if (CollectionUtils.isEmpty(folderIds)) {
            return Collections.emptyMap();
        }

        Map<String, Long> pageCountMap = folderIds.stream()
                .collect(Collectors.toMap(Function.identity(), id -> 0L));

        List<FolderPageCount> results = pageRepository.countPagesInFolders(folderIds);

        results.forEach(result -> pageCountMap.put(result.getFolderId(), result.getCount()));

        return pageCountMap;
    }

    public Map<String, List<Page>> getPagesForFolders(List<String> folderIds) {
        if (CollectionUtils.isEmpty(folderIds)) {
            return Collections.emptyMap();
        }
    
        List<Page> pages = pageRepository.findByFolderIdIn(folderIds);
    
        return pages.stream()
            .collect(Collectors.groupingBy(Page::getFolderId));
    }
}
