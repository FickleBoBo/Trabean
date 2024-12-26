package com.trabean.account.repository;

import com.trabean.account.domain.UserAccountRelation;
import com.trabean.account.domain.UserAccountRelation.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRelationRepository extends JpaRepository<UserAccountRelation, Long> {

    @Query("SELECT u FROM UserAccountRelation u WHERE u.userId = :userId AND u.account.accountId = :accountId")
    Optional<UserAccountRelation> findByUserIdAndAccountId(Long userId, Long accountId);

    @Query("SELECT u FROM UserAccountRelation u WHERE u.userId = :userId")
    Optional<List<UserAccountRelation>> findAllByUserId(Long userId);

    @Query("SELECT u FROM UserAccountRelation u WHERE u.account.accountId = :accountId")
    Optional<List<UserAccountRelation>> findAllByAccountId(Long accountId);

    @Modifying
    @Query("UPDATE UserAccountRelation u SET u.userRole = :userRole WHERE u.userId = :userId AND u.account.accountId = :accountId")
    void updateUserRoleByUserIdAndAccountId(Long userId, Long accountId, UserRole userRole);

    List<UserAccountRelation> findByAccount_AccountIdAndUserRole(Long accountId, UserRole userRole);
}
