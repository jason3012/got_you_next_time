package com.settleup.group;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
}
