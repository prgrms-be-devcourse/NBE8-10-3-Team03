package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.Chat;
import com.back.domain.chat.chat.entity.ChatImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatImageRepository extends JpaRepository<ChatImage, Integer> {
    List<ChatImage> findByChat(Chat chat);
}
