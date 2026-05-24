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
| — | Limit order (target price) | POST | `/api/orders/limit` | Yes |
| — | List / cancel limit orders | GET / DELETE | `/api/orders/limit` | Yes |

**Market trade (req. 3):** executes immediately at current `bestAsk` (BUY) or `bestBid` (SELL) from the database.

**Limit order:** scheduler every 2s updates prices then scans pending orders:

- **BUY** at `limitPrice` → fills when `bestAsk <= limitPrice` **or** `bestBid <= limitPrice` (market at/below your target). Execution at `min(ask, limit)`.
- **SELL** at `limitPrice` → fills when `bestBid >= limitPrice` **or** `bestAsk >= limitPrice`. Execution at `max(bid, limit)`.

USDT is reserved for limit buys; base asset is reserved for limit sells. Filled limit orders appear in trade history.

**Prices:** scheduler every **2s** fetches live **ETH/BTC** prices (Binance + Huobi), saves best bid/ask, then **scans all pending limit orders** and fills matches (ETH credited to wallet). Config: `crypto.price-aggregation.interval-ms: 2000`.

### Run Postman flow

1. Start app: `mvn spring-boot:run`
2. Import `postman/crypto-system.postman_collection.json`
3. **Limit orders:** open folder **2 - Limit order (1 order → match → ETH wallet)** → **Run folder** (5 steps: register → price → place order → verify matched → verify ETH in wallet).
4. Or: **0 - Prices** → **1 - Auth** → **Full flow** (market trade)

**Limit order folder tests:** pending buy (below ask) → cancel → immediate fill → SELL limit on BTC.

### Example trade body

```json
{
  "symbol": "ETHUSDT",
  "side": "BUY",
  "quantity": "0.10000000"
}
```

Pairs: `ETHUSDT`, `BTCUSDT`. Sides: `BUY`, `SELL`.

### Example limit buy (ETH @ 1999)

```json
{
  "symbol": "ETHUSDT",
  "side": "BUY",
  "quantity": "0.10000000",
  "limitPrice": "1999.00000000"
}
```

With live feeds enabled, place a limit buy at or above the current market ask to fill immediately, or below ask and wait for the market to move (checked every ~5s). You can also set `use-external-feeds: false` and edit `crypto.market.*` prices in yaml for offline demos.
