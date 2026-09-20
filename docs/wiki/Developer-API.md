# Developer API

Outposts is built with a decoupled domain API for third-party developers, custom addons, and scoreboard integration.

---

## 1. Dependency Setup

### Gradle
```groovy
repositories {
    mavenCentral()
    // Local or private repo
}

dependencies {
    compileOnly("me.goosbanny:outposts:1.0")
}
```

---

## 2. Querying Arenas & Snapshots

To maintain thread safety and prevent `ConcurrentModificationException` across Paper and Folia threads, external plugins query immutable snapshot records (`ArenaViewSnapshot`):

```java
import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.arena.ArenaView;
import me.goosbanny.outposts.api.arena.OutpostArena;

public void checkOutpost(String arenaId) {
    OutpostArena arena = Outposts.getInstance().getArenaManager().getArena(arenaId);
    if (arena == null) return;

    // Zero-lock, thread-safe DTO snapshot
    ArenaView view = arena.createSnapshot();
    System.out.println("Arena State: " + view.getState());
    System.out.println("Progress: " + view.getProgress() + "%");
    System.out.println("Controller: " + view.getControllerTeam());
    System.out.println("Is Contested: " + view.isContested());
}
```

---

## 3. Custom Event Listeners

Outposts fires dedicated Bukkit events during state transitions:

```java
import me.goosbanny.outposts.api.event.OutpostCaptureEvent;
import me.goosbanny.outposts.api.event.OutpostLostEvent;
import me.goosbanny.outposts.api.event.OutpostContestEvent;
import me.goosbanny.outposts.api.event.OutpostPreShiftEvent;
import me.goosbanny.outposts.api.event.OutpostShiftEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyOutpostListener implements Listener {

    @EventHandler
    public void onCapture(OutpostCaptureEvent event) {
        String teamName = event.getTeamName();
        String arenaId = event.getArena().getId();
        // Custom logic, discord webhook, or particles
    }

    @EventHandler
    public void onLost(OutpostLostEvent event) {
        String lostTeam = event.getLostTeamName();
        // Notify members or play sound
    }

    @EventHandler
    public void onContest(OutpostContestEvent event) {
        if (event.isContested()) {
            // Alarm sounds or tactical alerts
        }
    }

    @EventHandler
    public void onPreShift(OutpostPreShiftEvent event) {
        // Cancellable relocation event
        if (someCustomCondition()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onShift(OutpostShiftEvent event) {
        // Fired after region relocation succeeds
        System.out.println("Arena relocated to " + event.getToRegion().getId());
    }
}
```

---

## 4. Custom Team Roster Providers

You can register custom team implementations by implementing `TeamRosterProvider`:

```java
import me.goosbanny.outposts.Outposts;
import me.goosbanny.outposts.api.team.TeamRosterProvider;
import org.bukkit.entity.Player;

public class CustomClanProvider implements TeamRosterProvider {
    @Override public String getProviderName() { return "CustomClans"; }
    @Override public boolean hasTeam(Player player) { return ...; }
    @Override public String getTeamId(Player player) { return ...; }
    @Override public String getTeamName(Player player) { return ...; }
    @Override public UUID getTeamLeader(Player player) { return ...; }
    @Override public List<Player> getOnlineMembers(Player player) { return ...; }
    @Override public List<Player> getOnlineMembersById(String teamId) { return ...; } // Optional O(1) indexed lookup
    @Override public boolean areAllies(String teamA, String teamB) { return ...; }
}

// Registration
Outposts.getInstance().getTeamHookManager().setRosterProvider(new CustomClanProvider());
```
