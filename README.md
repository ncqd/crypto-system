# crypto-system

Crypto trading MVP — **ETHUSDT** and **BTCUSDT** only. USDT is the quote currency for settlement.

## Assignment APIs (Postman)

Import **`postman/crypto-system.postman_collection.json`**.

| # | Requirement | Method | Path | Auth |
|---|-------------|--------|------|------|
| — | Register | POST | `/api/auth/register` | No |
| — | Login | POST | `/api/auth/login` | No |
| 2 | Latest best aggregated price | GET | `/api/prices/latest` | No |
| 2 | Latest price by pair | GET | `/api/prices/latest/{symbol}` | No |
| 3 | Trade at aggregated price | POST | `/api/trades` | Yes |
| 4 | Wallet balances | GET | `/api/wallet/balances` | Yes |
| 5 | Trading history | GET | `/api/trades/history` | Yes |

**Trade (req. 3):** reads `bestAsk` for BUY and `bestBid` for SELL from the database only — **no third-party call when placing an order**.

**Prices:** aggregated every 10s from internal configured sources by default (`crypto.price-aggregation.use-external-feeds: false`). Set `use-external-feeds: true` to use Binance/Huobi HTTP feeds instead.

### Run Postman flow

1. Start app: `mvn spring-boot:run`
2. Import `postman/crypto-system.postman_collection.json`
3. Run **0 - Prices** — no login required
4. Run **1 - Auth** → Register (saves `accessToken`)
5. Run **Full flow** folder (deposit → trade → withdraw)

### Example trade body

```json
{
  "symbol": "ETHUSDT",
  "side": "BUY",
  "quantity": "0.10000000"
}
```

Pairs: `ETHUSDT`, `BTCUSDT`. Sides: `BUY`, `SELL`.
