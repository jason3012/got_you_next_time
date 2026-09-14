# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

SettleUp is for friend groups who share meals, trips, housing costs, and everyday purchases. People primarily use it from a phone, either while the group is together or shortly after a purchase, to identify a real charge, share it with the right friends, and see who owes whom without doing the arithmetic themselves.

## Product Purpose

SettleUp turns bank transaction data into group expenses. A user connects a bank account, reviews imported transactions, assigns a relevant transaction to a friend group, chooses how to split it, and later uses balances and settlement suggestions to close out what the group owes. Success means a real purchase can move from a bank feed to a clear, settled group balance with minimal manual entry or social friction.

## Positioning

Bank-transaction import is the product's main differentiator and the foundation of its purpose, not an optional convenience. SettleUp begins with purchases that actually occurred and lets users turn them into shared expenses. Manual expenses support the workflow, but the product should not present itself primarily as a manual ledger.

## Operating Context

The central workflow is: register or sign in, create or join a group, connect a bank through Plaid, synchronize and review transactions, convert a relevant transaction into a group expense, select equal, exact, or percentage splits, review balances, and record settlements. Users may also add an expense manually when a transaction is unavailable.

## Capabilities and Constraints

- The existing backend supports authenticated accounts, friend-group membership and roles, manual group expenses, equal/exact/percentage splits, balances, settlement plans, and recorded payments.
- Plaid Link provides bank connection; encrypted access tokens, account discovery, cursor-based synchronization, verified webhooks, and transaction-to-expense conversion already exist in the backend.
- The frontend stack is React, TypeScript, and Vite, with React Router and React Plaid Link already scaffolded. The project intends to use Fluent UI, Radix UI, and Lucide icons where they serve the interface.
- The product is mobile-web-first and must remain functional on tablet and desktop layouts.
- Bank and money actions must clearly distinguish synchronization, importing a transaction, splitting an expense, and recording a settlement.

## Brand Commitments

The product name is SettleUp. Its voice should be simple, informal, and friend-group oriented rather than corporate or traditionally financial. The user has made a conceptual-sketch identity, Notion-like simplicity, restrained clean motion, phone-first presentation, cream and near-black foundations, and earthy accents binding visual constraints; their detailed implementation belongs in the visual design record created after initialization.

## Evidence on Hand

The repository contains working backend APIs and tests for the listed capabilities plus typed frontend API contracts. No customer testimonials, usage metrics, partner endorsements beyond the implemented Plaid integration, production screenshots, or other marketing proof are available and none should be fabricated.

## Product Principles

1. Begin with real transaction data; the bank feed is the product's center of gravity.
2. Make sharing a purchase with friends faster than recreating it in a manual ledger.
3. Keep every amount, split, balance, and settlement understandable at a glance.
4. Reduce social friction by making group state and obligations explicit without sounding accusatory.
5. Preserve a complete manual path when a transaction is unavailable without letting it overshadow the primary workflow.
