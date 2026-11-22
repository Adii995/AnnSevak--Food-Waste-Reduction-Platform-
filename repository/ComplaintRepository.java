
package com.AnnSavak.AnnSavak.repository;

import com.AnnSavak.AnnSavak.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    List<Complaint> findByStatusOrderByCreatedAtDesc(String status);
}
