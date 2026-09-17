package com.yse.dev.Repository;
import java.util.Optional;
import com.yse.dev.Entity.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUserId(String userId);
    boolean existsByUserId(String userId);
    boolean existsByNickname(String nickname);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.userId = :userId")
    Optional<Member> findByUserIdForUpdate(@Param("userId") String userId);
    boolean existsByEmail(String email);
    Optional<Member> findByNameAndEmail(String name, String email);

}
