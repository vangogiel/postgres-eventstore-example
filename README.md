#postgres-eventstore-example

This application represents an example solution for an in-house built event store. It proves infrastructure can be implemented using well known tools to most Engineers. With appropriate understanding we can build an event store using Postgres and solid application layer. 

This way we gain more control over schema and queries and performance whilst reducing cost. We do have to mind building your own event store requires considerably more effort from the team.

In this example we will represent banking system which will have to manage accounts and transactions. For example you need to have an account open in order to deposit an amount. 

The traditional flow will represent:

Command -> Account Aggregate -> Domain Event -> Event Store -> Rehydration

For this simple exercise we will assume Event Storming exercise took place and the following are the outcomes:

Commands:
* OpenAccount
* DepositMoney
* WithdrawMoney
* CloseAccount

Events:
* AccountOpened
* MoneyDeposited
* MoneyWithdrawn
* WithdrawalRejected
* AccountClosed

Aggregate:
* Account
