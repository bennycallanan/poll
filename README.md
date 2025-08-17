# Poll Plugin

A feature-rich Minecraft poll plugin with GUI voting, SQLite storage, and staff management tools.

## Requirements & Building

### Requirements
- **Minecraft**: 1.21+ (Paper/Spigot)
- **Java**: 17 or higher
- **Dependencies**: cLibraries (included in shaded JAR)

### Building
```bash
git clone <repository-url>
cd poll
mvn clean package
```

## Commands

### Player Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/poll` | Open the poll list menu to view and vote on active polls | `poll.use` |
| `/poll list` | List all active polls in chat | `poll.use` |
| `/poll info <pollId>` | Show detailed information about a specific poll | `poll.use` |

### Staff Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/createpoll <duration> <question>` | Create a new poll with GUI option selection | `poll.create` |
| `/poll close <pollId>` | Close a specific poll (make it inactive) | `poll.close` |
| `/poll remove <pollId>` | Remove a poll completely from the database | `poll.remove` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `poll.use` | Allows players to view and vote on polls | `true` |
| `poll.create` | Allows staff to create new polls | `op` |
| `poll.close` | Allows staff to close polls | `op` |
| `poll.remove` | Allows staff to delete polls | `op` |