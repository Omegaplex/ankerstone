# Manual Multiplayer Credit Check

Run once for a new Minecraft port, and after changes to placement credit or login handling. This exercises real client connections without maintaining client-test code. It does not test Microsoft authentication or internet routing.

## Setup

Use a disposable dedicated server, not the lab backup or a personal world. One Prism instance can be launched alternately under two offline usernames; the players do not need to be connected simultaneously.

1. Use `\MinecraftTests\AnkerstoneNetwork-xx.x` on your test drive, replacing `xx.x` with the target Minecraft version. Create a fresh test world in that server folder. Do not copy an older server's libraries, versions, player data, or world.
2. Download the matching Minecraft/Fabric server launcher from [Fabric](https://fabricmc.net/use/server/). Rename the downloaded JAR to `fabric-server-launch.jar`; do not extract it.
3. Create a `mods` folder. Install the matching Fabric API and the exact Ankerstone JAR being tested. Use the same Ankerstone JAR in the client.
4. Use the Java version required by the target Minecraft release. From the server folder, run the command below. The launcher downloads its dependencies and initially stops at the EULA prompt.
5. Review the Minecraft EULA linked in `eula.txt`. If you accept it, set `eula=true`. Apply the server settings below before starting again.
6. Start the server with the same command and wait for Done. Stop any other test server using port 25566 first.
7. In Prism, select the matching Minecraft instance. Use **Launch Offline** and enter exactly `AnkerPlacer`. Use the same capitalization every time.
8. Connect through Multiplayer / Direct Connection to `127.0.0.1:25566`.

```powershell
java -Xms512M -Xmx2G -jar fabric-server-launch.jar nogui
```

Check `java -version` first. If it selects the wrong runtime, use the full path to the correct Java executable. An existing start script is fine if it uses that runtime and launcher from this server folder.

If the launcher asks for account setup, use its normal licensed-account flow. Do not modify authentication files to bypass it. If Launch Offline does not offer a username, use Prism's account settings to add/select the two offline profiles, where supported.

The server must retain these settings:

```properties
server-ip=127.0.0.1
server-port=25566
online-mode=false
enforce-secure-profile=false
enable-query=false
enable-rcon=false
management-server-enabled=false
pause-when-empty-seconds=0
white-list=false
gamemode=creative
allow-flight=true
spawn-protection=0
```

Offline mode permits impersonation. Keep the server bound to 127.0.0.1; do not forward the port, expose a tunnel, or change it to a LAN/public address.

Commands below are entered in the **server console**, without a leading slash. Players do not need operator permissions.

## Place While Off

With AnkerPlacer connected, teleport to the fixture area first:

```mcfunction
tp AnkerPlacer 1002.5 105 1004.5 -90 15
```

Wait for the surrounding chunks to appear, then prepare the platform and inventory:

```mcfunction
fill 1000 100 1000 1007 100 1007 minecraft:smooth_stone
fill 1000 101 1000 1007 103 1007 minecraft:air
tp AnkerPlacer 1002.5 101 1004.5 -90 15
clear AnkerPlacer
advancement revoke AnkerPlacer everything
give AnkerPlacer ankerstone:ankerstone 1
```

1. As AnkerPlacer, manually place the anchor at `1004 101 1004`. Do not use setblock: this check needs actual player placement attribution.
2. Confirm it is Off:

```mcfunction
execute if block 1004 101 1004 ankerstone:ankerstone[mode=off] run say PASS: placed anchor is Off
execute unless block 1004 101 1004 ankerstone:ankerstone[mode=off] run say FAIL: placement or mode is wrong
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=true}] run say FAIL: placer already has activation credit
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=false}] run say PASS: placer starts without activation credit
```

Disconnect and fully close the client. Confirm the server logs AnkerPlacer leaving before continuing.

## Activate As Someone Else

Launch the same Prism instance offline as exactly `AnkerActivator` and connect to the same address. In the server console:

```mcfunction
tp AnkerActivator 1002.5 101 1004.5 -90 15
clear AnkerActivator
advancement revoke AnkerActivator everything
give AnkerActivator minecraft:redstone_block 1
list
```

The list must show only AnkerActivator. Place the redstone block at `1005 101 1004`, directly east of the anchor. Then check:

```mcfunction
execute if block 1004 101 1004 ankerstone:ankerstone[mode=wide] run say PASS: another player activated the anchor
execute unless block 1004 101 1004 ankerstone:ankerstone[mode=wide] run say FAIL: anchor did not activate
execute if entity @a[name=AnkerActivator,advancements={ankerstone:stay_a_while=true}] run say FAIL: activator stole placer credit
execute if entity @a[name=AnkerActivator,advancements={ankerstone:stay_a_while=false}] run say PASS: activator did not receive placer credit
```

Remove the activated anchor and its power in the console:

```mcfunction
setblock 1004 101 1004 minecraft:air
setblock 1005 101 1004 minecraft:air
```

Disconnect and close the client. Enter `stop` in the server console and wait for the process to finish saving and exit. This deliberately checks that pending credit survives both block removal and a disk restart.

## Return After Restart

1. Start the same server again using the launch command or your start script, and wait for Done.
2. Launch offline as AnkerPlacer and reconnect. Do not place or activate another anchor.
3. With AnkerPlacer connected, check:

```mcfunction
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=true}] run say PASS: saved credit reached the returning placer
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=false}] run say FAIL: returning placer did not receive saved credit
```

The server-side checks are authoritative; a missing toast alone is not a failed award.

## No Duplicate Delivery

While AnkerPlacer is still connected, revoke only the activation advancement:

```mcfunction
advancement revoke AnkerPlacer only ankerstone:stay_a_while
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=false}] run say PASS: revocation confirmed
```

1. Disconnect and close the client.
2. Stop the server cleanly, then restart it.
3. Reconnect as AnkerPlacer without placing or activating anything.
4. Check again:

```mcfunction
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=false}] run say PASS: consumed credit was not delivered again
execute if entity @a[name=AnkerPlacer,advancements={ankerstone:stay_a_while=true}] run say FAIL: consumed credit was delivered again
```

Passing all stages completes this check. Keep the server log and record the date, Minecraft version, Loader/API versions, and exact Ankerstone JAR in testing.md. No output from both alternatives is not a pass: verify the named player is connected. Stop at any FAIL and preserve the world and log for investigation.

## Repeating The Check

For another Minecraft version, repeat Setup in its own `\MinecraftTests\AnkerstoneNetwork-xx.x` folder with matching dependencies and clients. For another run on the same version, use a fresh disposable world so old attribution or pending credit cannot affect the result.

Keep both usernames and the fixture coordinates unchanged. The commands need changing only if Minecraft command syntax or Ankerstone's IDs/states change. Record version-specific results in [Testing](testing.md), not in this procedure.
