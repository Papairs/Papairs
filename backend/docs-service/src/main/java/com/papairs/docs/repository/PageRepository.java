package com.papairs.docs.repository;

import com.papairs.docs.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PageRepository extends JpaRepository<Page, String> {

    /**
     * Find pages by owner ID
     * @param ownerId owner's user ID
     * @return List of pages owned by the user
     */
    List<Page> findByOwnerId(String ownerId);
}
