package com.yse.dev.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yse.dev.Entity.MemberPreference;

public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, Long> {
    Optional<MemberPreference> findByUserId(String userId);
    void deleteByUserId(String userId);
}
