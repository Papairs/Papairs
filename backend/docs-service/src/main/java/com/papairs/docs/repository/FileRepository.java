package com.papairs.docs.repository;

import com.papairs.docs.model.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<UserFile, String> {

    List<UserFile> findByUserId(String userId);

    boolean existsByUserIdAndFilename(String userId, String filename);
}
