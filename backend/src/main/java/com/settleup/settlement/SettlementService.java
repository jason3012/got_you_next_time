package com.settleup.settlement;

import com.settleup.common.Money;
import com.settleup.common.exception.ValidationException;
import com.settleup.expense.Expense;
import com.settleup.expense.ExpenseRepository;
import com.settleup.group.Group;
import com.settleup.group.GroupMember;
import com.settleup.group.GroupMemberRepository;
import com.settleup.group.GroupService;
import com.settleup.settlement.BalanceCalculator.ExpenseEntry;
import com.settleup.settlement.BalanceCalculator.SettlementEntry;
import com.settleup.settlement.BalanceCalculator.Share;
import com.settleup.settlement.dto.BalanceResponse;
import com.settleup.settlement.dto.RecordSettlementRequest;
import com.settleup.settlement.dto.SettlementResponse;
import com.settleup.settlement.dto.TransferResponse;
import com.settleup.user.User;
import com.settleup.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SettlementService {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final BalanceCalculator balanceCalculator = new BalanceCalculator();
    private final SettlementPlanner settlementPlanner = new SettlementPlanner();

    public SettlementService(
            GroupService groupService,
            GroupMemberRepository groupMemberRepository,
            ExpenseRepository expenseRepository,
            SettlementRepository settlementRepository,
            UserRepository userRepository
    ) {
        this.groupService = groupService;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> balances(UUID groupId, UUID userId) {
        groupService.requireMember(groupId, userId);
        Map<UUID, Money> balances = calculateBalances(groupId);
        Map<UUID, String> names = groupMemberRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        member -> member.getUser().getId(),
                        member -> member.getUser().getDisplayName()));
        return balances.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new BalanceResponse(entry.getKey(), names.get(entry.getKey()), entry.getValue().cents()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> plan(UUID groupId, UUID userId) {
        groupService.requireMember(groupId, userId);
        return settlementPlanner.plan(calculateBalances(groupId)).stream()
                .map(transfer -> new TransferResponse(
                        transfer.fromUserId(), transfer.toUserId(), transfer.amount().cents()))
                .toList();
    }

    @Transactional
    public SettlementResponse record(UUID groupId, UUID userId, RecordSettlementRequest request) {
        Group group = groupService.requireMember(groupId, userId).getGroup();
        if (request.fromUserId().equals(request.toUserId())) {
            throw new ValidationException("Settlement users must be different");
        }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, request.fromUserId())
                || !groupMemberRepository.existsByGroupIdAndUserId(groupId, request.toUserId())) {
            throw new ValidationException("Settlement users must be members of the group");
        }

        Map<UUID, Money> balances = calculateBalances(groupId);
        long fromBalance = balances.get(request.fromUserId()).cents();
        long toBalance = balances.get(request.toUserId()).cents();
        if (fromBalance >= 0 || toBalance <= 0) {
            throw new ValidationException("Settlement must go from a debtor to a creditor");
        }
        long maximum = Math.min(Math.negateExact(fromBalance), toBalance);
        if (request.amountCents() > maximum) {
            throw new ValidationException("Settlement amount exceeds the outstanding balance");
        }

        User fromUser = userRepository.getReferenceById(request.fromUserId());
        User toUser = userRepository.getReferenceById(request.toUserId());
        Settlement settlement = settlementRepository.saveAndFlush(
                new Settlement(group, fromUser, toUser, new Money(request.amountCents())));
        return toResponse(settlement);
    }

    private Map<UUID, Money> calculateBalances(UUID groupId) {
        List<GroupMember> members = groupMemberRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId);
        List<ExpenseEntry> expenses = expenseRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toEntry)
                .toList();
        List<SettlementEntry> settlements = settlementRepository.findAllByGroupIdOrderByCreatedAtAsc(groupId)
                .stream()
                .map(settlement -> new SettlementEntry(
                        settlement.getFromUser().getId(),
                        settlement.getToUser().getId(),
                        settlement.getAmount()))
                .toList();
        return balanceCalculator.calculate(
                members.stream().map(member -> member.getUser().getId()).toList(),
                expenses,
                settlements);
    }

    private ExpenseEntry toEntry(Expense expense) {
        List<Share> shares = expense.getSplits().stream()
                .sorted(Comparator.comparing(split -> split.getUser().getId()))
                .map(split -> new Share(split.getUser().getId(), split.getShare()))
                .toList();
        return new ExpenseEntry(expense.getPaidBy().getId(), expense.getAmount(), shares);
    }

    private SettlementResponse toResponse(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getGroup().getId(),
                settlement.getFromUser().getId(),
                settlement.getToUser().getId(),
                settlement.getAmount().cents(),
                settlement.getCreatedAt());
    }
}
