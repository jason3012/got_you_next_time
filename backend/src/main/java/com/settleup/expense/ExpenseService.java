package com.settleup.expense;

import com.settleup.common.Money;
import com.settleup.common.exception.ForbiddenException;
import com.settleup.common.exception.NotFoundException;
import com.settleup.expense.SplitCalculator.CalculatedSplit;
import com.settleup.expense.SplitCalculator.SplitInput;
import com.settleup.expense.dto.CreateExpenseRequest;
import com.settleup.expense.dto.ExpenseResponse;
import com.settleup.expense.dto.ExpenseSplitRequest;
import com.settleup.expense.dto.ExpenseSplitResponse;
import com.settleup.group.Group;
import com.settleup.group.GroupMemberRepository;
import com.settleup.group.GroupService;
import com.settleup.user.User;
import com.settleup.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupService groupService;
    private final SplitCalculator splitCalculator = new SplitCalculator();

    public ExpenseService(
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            GroupMemberRepository groupMemberRepository,
            GroupService groupService
    ) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupService = groupService;
    }

    @Transactional
    public ExpenseResponse create(UUID groupId, UUID actingUserId, CreateExpenseRequest request) {
        Group group = groupService.requireMember(groupId, actingUserId).getGroup();
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, request.payerId())) {
            throw new ForbiddenException("Payer must be a member of the group");
        }

        List<SplitInput> splitInputs = request.splits().stream()
                .map(this::toSplitInput)
                .toList();
        List<CalculatedSplit> calculatedSplits = splitCalculator.calculate(
                new Money(request.amountCents()), request.splitStrategy(), splitInputs);

        for (CalculatedSplit split : calculatedSplits) {
            if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, split.userId())) {
                throw new ForbiddenException("Every split participant must be a group member");
            }
        }

        Map<UUID, User> users = loadUsers(calculatedSplits.stream().map(CalculatedSplit::userId).toList());
        User payer = userRepository.findById(request.payerId())
                .orElseThrow(() -> new NotFoundException("Payer was not found"));
        User creator = userRepository.findById(actingUserId)
                .orElseThrow(() -> new NotFoundException("Expense creator was not found"));

        Expense expense = new Expense(
                group,
                payer,
                creator,
                request.description().trim(),
                new Money(request.amountCents()),
                request.splitStrategy());
        calculatedSplits.forEach(split -> expense.addSplit(
                new ExpenseSplit(expense, users.get(split.userId()), split.amount())));
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(UUID groupId, UUID userId) {
        groupService.requireMember(groupId, userId);
        return expenseRepository.findAllByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID groupId, UUID userId, UUID expenseId) {
        groupService.requireMember(groupId, userId);
        Expense expense = expenseRepository.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new NotFoundException("Expense was not found"));
        expenseRepository.delete(expense);
    }

    private SplitInput toSplitInput(ExpenseSplitRequest request) {
        Money amount = request.amountCents() == null ? null : new Money(request.amountCents());
        return new SplitInput(request.userId(), amount, request.percentage());
    }

    private Map<UUID, User> loadUsers(List<UUID> userIds) {
        Map<UUID, User> users = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> users.put(user.getId(), user));
        if (users.size() != userIds.size()) {
            throw new NotFoundException("One or more split participants were not found");
        }
        return users;
    }

    private ExpenseResponse toResponse(Expense expense) {
        List<ExpenseSplitResponse> splits = expense.getSplits().stream()
                .sorted(Comparator.comparing(split -> split.getUser().getId()))
                .map(split -> new ExpenseSplitResponse(split.getUser().getId(), split.getShare().cents()))
                .toList();
        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getPaidBy().getId(),
                expense.getCreatedBy().getId(),
                expense.getDescription(),
                expense.getAmount().cents(),
                expense.getSplitStrategy(),
                splits,
                expense.getCreatedAt());
    }
}
