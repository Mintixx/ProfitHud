# Profit HUD

Track your money, profits, expenses, and inventory value directly on your Minecraft screen. Profit HUD adds a clean, fully customizable economy HUD to your game. It reads payment messages from chat, tracks your income and spending, calculates your net profit, and estimates the value of your inventory.

See exactly how much money you're making, how much you're spending, and how valuable your items are without constantly checking everything manually.

Client-side only. You install it, the server doesn't need to.

## Features

### Profit HUD

- Displays your current balance (if detectable), earnings, expenses, and net profit
- Tracks profit per minute and per hour
- Shows your current session duration
- Live updates when payments are detected
- Customizable HUD with a live preview in the menu

### Payment tracking

- Automatically detects incoming and outgoing `/pay` messages
- Tracks who paid you and who you paid
- Supports K, M, B, and T number formats
- Keeps a history of detected transactions
- Calculates total earnings, spending, and net profit
- Ignore specific players

### Inventory value

- Scans your inventory and calculates its estimated value
- Search every item in the game
- Set custom prices for individual items
- Automatically updates when inventory contents change
- Calculates total inventory value based on item quantities
- Import and export item prices

### Separate financial statistics

Confirmed transactions and estimated inventory value are tracked separately.

Changing an item's estimated price will never affect your confirmed earnings, expenses, or net profit.

Your inventory value is an estimate, not money you've actually earned.

### In-game menu (Right Shift)

Press Right Shift to open the menu. You can rebind it in Controls. It's also available through Mod Menu and `/profit`.

<img width="1000" height="500" alt="image" src="https://github.com/user-attachments/assets/458791d4-e629-46fb-8d88-9c0cfa11e2ce" />

<img width="1919" height="1005" alt="image" src="https://github.com/user-attachments/assets/57d354d3-d4e5-465c-9d01-24cc8c6e4b96" />


| Tab             | What you can change                                                        |
| --------------- | -------------------------------------------------------------------------- |
| Overview        | Balance, earnings, expenses, net profit, inventory value, session duration |
| Live Tracker    | Payment tracking, transaction history, ignored players                     |
| Inventory Value | Inventory scanner, item quantities, estimated total value                  |
| Item Prices     | Search items, set custom prices, import and export prices                  |
| Session History | Previous sessions, earnings, expenses, session duration                    |
| Statistics      | Total earnings, spending, highest payment, average payment                 |
| Layout          | 9 screen anchors, offset, scale, width, height, padding, drag-to-position  |
| Style           | Corner radius, border, gradient, drop shadow, accent stripe                |
| Colors          | 8 presets, master opacity, custom colors with RGBA sliders                 |
| Text            | Title, currency symbol, number format, text scale                          |
| Effects         | New-payment flash, profit animation, transitions, notification sounds      |
| Advanced        | Custom payment regex, raw chat debug, test messages, reset                 |

### Session tracking

- Track earnings and expenses during your session
- Calculate confirmed net profit
- View your highest incoming and outgoing payments
- Track average payment amounts
- Start new sessions or reset statistics
- Export session statistics to CSV

### Customization

- 9 screen positions
- Adjustable HUD scale and size
- Custom colors and gradients
- Adjustable opacity and borders
- Custom titles and currency symbols
- Animated profit counters
- Live preview while editing

### Quick start

1. Install Fabric Loader and Fabric API (Mod Menu is optional).
2. Launch Minecraft and press Right Shift.
3. Open the Item Prices tab and configure your item prices.
4. Start playing and receive or send payments.
5. Profit HUD automatically tracks detected transactions.
6. Check Overview to see your earnings, expenses, net profit, and estimated inventory value.

### Commands

- `/profit` opens the menu
- `/profit reset` resets session statistics
- `/profit session` shows session statistics
- `/profit prices` opens item prices
- `/profit hud` opens HUD customization
- `/profit export` exports statistics

### Setup note

The mod detects payments by matching the chat messages your server displays when someone pays you.

Different servers use different message formats. If payments aren't detected, open Advanced, enable raw chat debugging, and configure the payment regex to match your server's messages.

Your actual server balance is only displayed if it can be reliably detected. Profit calculations are based on confirmed transactions observed by the mod, not guessed balance values.

Item prices are manually configurable because server economies have different prices.

### Requirements

- Minecraft 26.2
- Fabric Loader
- Fabric API
- Java version required by Minecraft 26.2
- Mod Menu (optional)

### Disclaimer

Profit HUD only reads client-visible game information, tracks detected chat transactions, scans your inventory, and draws an overlay. It never automates gameplay or sends commands automatically.

Estimated inventory values are not guaranteed to match actual server market prices. Transaction tracking may be incomplete if payments are not visible in chat.

Check your server's rules on client-side mods before using it.

This mod is not affiliated with or endorsed by DonutSMP or any other Minecraft server.

**Made by Mintixx\_**
