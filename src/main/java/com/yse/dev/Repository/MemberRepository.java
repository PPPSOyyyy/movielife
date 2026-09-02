package com.yse.dev.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.Member;

public interface MemberRepository
        extends JpaRepository<Member, Long> {

    Optional<Member> findByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsByNickname(String nickname);
}