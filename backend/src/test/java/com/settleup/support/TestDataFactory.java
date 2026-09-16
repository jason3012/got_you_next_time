package com.settleup.support;

import com.settleup.common.Money;
import com.settleup.expense.Expense;
import com.settleup.expense.ExpenseRepository;
import com.settleup.expense.ExpenseSplit;
import com.settleup.expense.SplitStrategy;
import com.settleup.group.Group;
import com.settleup.group.GroupMember;
import com.settleup.group.GroupMemberRepository;
import com.settleup.group.GroupMemberRole;
import com.settleup.group.GroupRepository;
import com.settleup.settlement.Settlement;
import com.settleup.settlement.SettlementRepository;
import com.settleup.user.User;
import com.settleup.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

public final class TestDataFactory {

    private final UserRepository users;
    private final GroupRepository groups;
    private final GroupMemberRepository members;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;
    private final PasswordEncoder passwordEncoder;

    TestDataFactory(
            UserRepository users,
            GroupRepository groups,
            GroupMemberRepository members,
            ExpenseRepository expenses,
            SettlementRepository settlements,
            PasswordEncoder passwordEncoder
    ) {
        this.users = users;
        this.groups = groups;
        this.members = members;
        this.expenses = expenses;
        this.settlements = settlements;
        this.passwordEncoder = passwordEncoder;
    }

    public User user(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        return users.saveAndFlush(new User(slug + "@example.com", name, passwordEncoder.encode("password-123")));
    }

    public Group group(String name, User admin, User... additionalMembers) {
        Group group = groups.saveAndFlush(new Group(name, admin));
        members.save(new GroupMember(group, admin, GroupMemberRole.ADMIN));
        Arrays.stream(additionalMembers)
                .forEach(user -> members.save(new GroupMember(group, user, GroupMemberRole.MEMBER)));
        members.flush();
        return group;
    }

    public Expense equalExpense(Group group, User payer, User creator, long cents, User... participants) {
        Expense expense = new Expense(group, payer, creator, "Shared expense", new Money(cents), SplitStrategy.EQUAL);
        long base = cents / participants.length;
        long remainder = cents % participants.length;
        for (int index = 0; index < participants.length; index++) {
            long share = base + (index < remainder ? 1 : 0);
            expense.addSplit(new ExpenseSplit(expense, participants[index], new Money(share)));
        }
        return expenses.saveAndFlush(expense);
    }

    public Settlement settlement(Group group, User from, User to, long cents) {
        return settlements.saveAndFlush(new Settlement(group, from, to, new Money(cents)));
    }
}
