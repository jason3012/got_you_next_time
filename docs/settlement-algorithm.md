# Settlement algorithm

SettleUp calculates one net balance for each group member:

```text
net balance = total paid - total owed
```

A positive balance means the member should receive money. A negative balance means the member owes money. Every expense split must add up to the original expense, so all group balances must add up to zero.

Recorded settlements update future balances. A payment from a debtor increases that member's balance and decreases the recipient's balance by the same number of cents.

## Transfer planning

The planner keeps debtors and creditors in separate priority queues. Each queue places the largest remaining amount first. The planner repeatedly matches the largest debtor with the largest creditor and transfers the smaller of their two balances. A member leaves the queue when the remaining balance reaches zero.

For `n` members with nonzero balances, this process creates at most `n - 1` transfers. Each priority queue operation costs `O(log n)`, which gives the planner `O(n log n)` time complexity.

Finding the absolute minimum number of transfers is NP-hard for the general case. The greedy method is fast, predictable, and produces a compact plan without an expensive search.

## Exact cents

All amounts use signed 64-bit integer cents. Split calculations distribute leftover cents in stable participant order. Balance calculations reject any expense whose shares do not equal its total. Settlement planning also rejects a balance set whose sum is not zero.
