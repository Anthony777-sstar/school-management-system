package com.crestwood.school_management_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.crestwood.school_management_system.domain.entity.NoticeboardPost;

public interface NoticeboardPostRepository extends JpaRepository<NoticeboardPost, Long> {
	List<NoticeboardPost> findAllByOrderByPostedAtDesc();
}
