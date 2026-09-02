package com.yse.dev.Repository;

import com.yse.dev.Entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<어떤 엔티티를 다룰지, 그 엔티티의 PK 타입>
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 1. 아이디로 회원 정보 찾기 (로그인할 때 사용)
    Optional<Member> findByUserId(String userId);

    // 2. 해당 아이디가 이미 존재하는지 확인 (아이디 중복확인 시 사용)
    boolean existsByUserId(String userId);

    // 3. 해당 닉네임이 이미 존재하는지 확인 (닉네임 중복확인 시 사용)
    boolean existsByNickname(String nickname);
}