#postgres-eventstore-example

This application represents an example solution for an in-house built event store. It proves infrastructure can be implemented using well known tools to most Engineers. With appropriate understanding we can build an event store using Postgres and an application layer. 
This way we gain control over schema and queries and performance whilst reducing cost. We do have to mind, building your own event store requires considerably more effort from the team.

In this example we will represent banking system which will have to manage accounts and transactions. For example, you need to have an account open in order to deposit an amount. 

The traditional flow will represent:

Command -> Account Aggregate -> Domain Event -> Event Store -> Rehydration

The domain model has been simplified by representing the account and its 
balance as a single aggregate. In practice, the balance is derived entirely from 
the account's event history (account opening and subsequent money movement events),
and both must remain strongly consistent. Combining them into a single aggregate 
provides a single source of truth, simplifies event rehydration, and avoids 
coordinating consistency across multiple aggregates.

The assumptions are that the model operates in a single currency for simplicity.

For this simple exercise we will assume Event Storming exercise took place and the following are the outcomes:

Commands:
* OpenAccount
* DepositMoney
* WithdrawMoney
* TransferMoney

Events:
* AccountOpened
* MoneyDeposited
* MoneyWithdrawn
* MoneyTransferSent
* MoneyTransferReceived

Aggregate:
```Scala
case class AccountAggregate(
  accountId: String,
  balance: BigDecimal,
  version: Long,
  state: State,
  behaviour: Behaviour
)
```

Events:
```
AccountOpened(
    id: String
)
```

```
MoneyDeposited(
    accountId: String,
    amount: BigDecimal
)
```

```
MoneyWithdrawn(
    accountId: String,
    amount: BigDecimal
)
```

```
MoneyTransferSent {
    sourceAccountId: String
    destinationAccountId: String,
    amount: BigDecimal
}
```

```
MoneyTransferReceived {
    sourceAccountId: String
    destinationAccountId: String,
    amount: BigDecimal
}
```
