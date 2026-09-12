package com.settleup.group;

import com.settleup.common.exception.ConflictException;
import com.settleup.common.exception.ForbiddenException;
import com.settleup.common.exception.NotFoundException;
import com.settleup.common.exception.ValidationException;
import com.settleup.expense.ExpenseRepository;
import com.settleup.expense.ExpenseSplitRepository;
import com.settleup.group.dto.AddGroupMemberRequest;
import com.settleup.group.dto.CreateGroupRequest;
import com.settleup.group.dto.GroupMemberResponse;
import com.settleup.group.dto.GroupResponse;
import com.settleup.settlement.SettlementRepository;
import com.settleup.user.User;
import com.settleup.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final SettlementRepository settlementRepository;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            UserRepository userRepository,
            ExpenseRepository expenseRepository,
            ExpenseSplitRepository expenseSplitRepository,
            SettlementRepository settlementRepository
    ) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public GroupResponse create(UUID userId, CreateGroupRequest request) {
        User creator = requireUser(userId);
        Group group = groupRepository.saveAndFlush(new Group(request.name().trim(), creator));
        groupMemberRepository.saveAndFlush(new GroupMember(group, creator, GroupMemberRole.ADMIN));
        return toResponse(group);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> listForUser(UUID userId) {
        requireUser(userId);
        return groupMemberRepository.findAllByUserId(userId).stream()
                .map(GroupMember::getGroup)
                .distinct()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse get(UUID groupId, UUID userId) {
        return toResponse(requireMember(groupId, userId).getGroup());
    }

    @Transactional
    public GroupResponse addMember(UUID groupId, UUID actingUserId, AddGroupMemberRequest request) {
        Group group = requireAdmin(groupId, actingUserId).getGroup();
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new NotFoundException("No account was found for this email"));
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new ConflictException("User is already a member of this group");
        }
        groupMemberRepository.saveAndFlush(new GroupMember(group, user, request.role()));
        return toResponse(group);
    }

    @Transactional
    public void removeMember(UUID groupId, UUID actingUserId, UUID memberUserId) {
        Group group = requireAdmin(groupId, actingUserId).getGroup();
        GroupMember membership = groupMemberRepository.findByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new NotFoundException("Group member was not found"));
        if (group.getCreatedBy().getId().equals(memberUserId)) {
            throw new ValidationException("The group creator cannot be removed");
        }
        boolean hasExpenseHistory = expenseRepository.existsByGroupIdAndPaidById(groupId, memberUserId)
                || expenseRepository.existsByGroupIdAndCreatedById(groupId, memberUserId)
                || expenseSplitRepository.existsByExpenseGroupIdAndUserId(groupId, memberUserId)
                || settlementRepository.existsByGroupIdAndFromUserId(groupId, memberUserId)
                || settlementRepository.existsByGroupIdAndToUserId(groupId, memberUserId);
        if (hasExpenseHistory) {
            throw new ConflictException("A member with expense history cannot be removed");
        }
        groupMemberRepository.delete(membership);
    }

    @Transactional(readOnly = true)
    public GroupMember requireMember(UUID groupId, UUID userId) {
        requireGroup(groupId);
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("User is not a member of this group"));
    }

    @Transactional(readOnly = true)
    public Group requireGroup(UUID groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group was not found"));
    }

    @Transactional(readOnly = true)
    public User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
    }

    private GroupMember requireAdmin(UUID groupId, UUID userId) {
        GroupMember membership = requireMember(groupId, userId);
        if (membership.getRole() != GroupMemberRole.ADMIN) {
            throw new ForbiddenException("Group admin access is required");
        }
        return membership;
    }

    private GroupResponse toResponse(Group group) {
        List<GroupMemberResponse> members = groupMemberRepository
                .findAllByGroupIdOrderByCreatedAtAsc(group.getId()).stream()
                .sorted(Comparator.comparing(member -> member.getUser().getId()))
                .map(member -> new GroupMemberResponse(
                        member.getUser().getId(),
                        member.getUser().getDisplayName(),
                        member.getUser().getEmail(),
                        member.getRole()))
                .toList();
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getCreatedBy().getId(),
                members,
                group.getCreatedAt(),
                group.getUpdatedAt());
    }
}
