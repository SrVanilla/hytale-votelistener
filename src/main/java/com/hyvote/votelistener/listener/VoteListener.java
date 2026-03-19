package com.hyvote.votelistener.listener;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.hyvote.plugins.votifier.event.VoteEvent;
import org.hyvote.plugins.votifier.vote.Vote;
import com.hyvote.votelistener.HytaleVoteListener;
import com.hyvote.votelistener.config.Config;
import com.hyvote.votelistener.config.MilestoneBonus;
import com.hyvote.votelistener.config.RandomReward;
import com.hyvote.votelistener.config.StreakBonus;
import com.hyvote.votelistener.data.PendingReward;
import com.hyvote.votelistener.data.PendingRewardsManager;
import com.hyvote.votelistener.data.PlayerVoteData;
import com.hyvote.votelistener.data.VoteDataManager;
import com.hyvote.votelistener.reward.RewardSelector;
import com.hyvote.votelistener.util.PlaceholderProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Listens for vote events from HytaleVotifier and handles reward delivery.
 *
 * This class subscribes to VoteEvent and executes configured commands
 * when a player votes on a voting site.
 */
public class VoteListener {
    private final HytaleVoteListener plugin;
    private final HytaleLogger logger;
    private final Config config;
    private final VoteDataManager voteDataManager;
    private final PendingRewardsManager pendingRewardsManager;

    /**
     * Creates a new VoteListener.
     *
     * @param plugin The plugin instance
     * @param config The configuration containing command list and settings
     * @param voteDataManager The vote data manager for tracking streaks and statistics
     * @param pendingRewardsManager The pending rewards manager for offline player rewards
     */
    public VoteListener(HytaleVoteListener plugin, Config config, VoteDataManager voteDataManager,
                        PendingRewardsManager pendingRewardsManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.config = config;
        this.voteDataManager = voteDataManager;
        this.pendingRewardsManager = pendingRewardsManager;
    }

    /**
     * Checks if a player is currently online on the server.
     *
     * @param username The username to check
     * @return true if the player is online, false otherwise
     */
    private boolean isPlayerOnline(String username) {
        // Check if player is connected via universe
        return Universe.get().getPlayers().stream()
                .anyMatch(ref -> ref.getUsername().equalsIgnoreCase(username));
    }

    /**
     * Finds a PlayerRef by username for online players.
     *
     * @param username The username to find
     * @return Optional containing the PlayerRef if online
     */
    private Optional<PlayerRef> findPlayerRef(String username) {
        return Universe.get().getPlayers().stream()
                .filter(ref -> ref.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    /**
     * Executes a command through the command manager.
     *
     * @param command The command string to execute
     */
    private void executeCommand(String command) {
        // Use console command execution
        CommandManager.get().handleCommand(ConsoleSender.INSTANCE, command);
    }

    /**
     * Handles incoming vote events from HytaleVotifier.
     *
     * @param event The vote event containing vote details
     */
    public void onVote(VoteEvent event) {
        Vote vote = event.getVote();
        String username = vote.username();
        String serviceName = vote.serviceName();

        // Look up UUID from online player, or use username as fallback key
        String uuid = findPlayerRef(username)
                .map(ref -> ref.getUuid().toString())
                .orElse(username);

        logger.at(Level.INFO).log("Vote received from %s for player: %s", serviceName, username);

        // Record vote and get updated player data with streak info
        PlayerVoteData playerData = voteDataManager.recordVote(uuid, username);
        playerData.addVoteSite(serviceName);

        logger.at(Level.INFO).log("Player %s voted on: %s", username, serviceName);

        if (playerData.hasVotedOnAllSites()) {
            logger.at(Level.INFO).log("Player %s completed all 3 votes!", username);
            executeCommand("nick color " + username);
            playerData.resetVoteSites();
        }

        int currentStreak = playerData.getCurrentStreak();
        int totalVotes = playerData.getTotalVotes();

        // Collect all reward commands before execution
        List<String> allCommands = new ArrayList<>();

        // Add base commands with placeholder replacement
        List<String> commands = config.getCommands();
        for (String command : commands) {
            String processedCommand = PlaceholderProcessor.process(command, vote, null, currentStreak, totalVotes, uuid);
            allCommands.add(processedCommand);

            if (config.isDebugMode()) {
                logger.at(Level.INFO).log("[Debug] Queued command: %s", processedCommand);
            }
        }

        // Add random reward commands if enabled
        if (config.isRandomRewardsEnabled()) {
            RandomReward selectedReward = RewardSelector.select(config.getRandomRewards());
            if (selectedReward != null) {
                logger.at(Level.INFO).log("Selected random reward: %s", selectedReward.getName());

                for (String rewardCommand : selectedReward.getCommands()) {
                    String processedRewardCommand = PlaceholderProcessor.process(
                        rewardCommand, vote, selectedReward.getName(), currentStreak, totalVotes, uuid);
                    allCommands.add(processedRewardCommand);

                    if (config.isDebugMode()) {
                        logger.at(Level.INFO).log("[Debug] Queued reward command: %s", processedRewardCommand);
                    }
                }
            }
        }

        // Add streak bonus commands if enabled
        if (config.isStreakBonusEnabled()) {
            for (StreakBonus streakBonus : config.getStreakBonuses()) {
                if (currentStreak == streakBonus.getStreakDays()) {
                    logger.at(Level.INFO).log("Awarding streak bonus: %s", streakBonus.getName());

                    for (String bonusCommand : streakBonus.getCommands()) {
                        String processedBonusCommand = PlaceholderProcessor.process(
                            bonusCommand, vote, streakBonus.getName(), currentStreak, totalVotes, uuid);
                        allCommands.add(processedBonusCommand);

                        if (config.isDebugMode()) {
                            logger.at(Level.INFO).log("[Debug] Queued streak bonus command: %s", processedBonusCommand);
                        }
                    }
                    break; // Only award one streak bonus per vote
                }
            }
        }

        // Add milestone bonus commands if enabled
        if (config.isMilestoneBonusEnabled()) {
            for (MilestoneBonus milestoneBonus : config.getMilestoneBonuses()) {
                if (totalVotes == milestoneBonus.getVotesRequired()) {
                    logger.at(Level.INFO).log("Awarding milestone bonus: %s", milestoneBonus.getName());

                    for (String bonusCommand : milestoneBonus.getCommands()) {
                        String processedBonusCommand = PlaceholderProcessor.process(
                            bonusCommand, vote, milestoneBonus.getName(), currentStreak, totalVotes, uuid);
                        allCommands.add(processedBonusCommand);

                        if (config.isDebugMode()) {
                            logger.at(Level.INFO).log("[Debug] Queued milestone bonus command: %s", processedBonusCommand);
                        }
                    }
                    break; // Only award one milestone bonus per vote
                }
            }
        }

        // Check if player is online and either execute or queue rewards
        if (isPlayerOnline(username)) {
            // Player is online - execute all commands immediately
            for (String cmd : allCommands) {
                executeCommand(cmd);

                if (config.isDebugMode()) {
                    logger.at(Level.INFO).log("[Debug] Executed command: %s", cmd);
                }
            }
        } else {
            // Player is offline - queue rewards for later delivery
            PendingReward pendingReward = new PendingReward(
                uuid,
                username,
                serviceName,
                System.currentTimeMillis(),
                allCommands
            );
            pendingRewardsManager.addPendingReward(uuid, pendingReward);
            logger.at(Level.INFO).log("Player %s is offline, queued %d reward commands for later delivery",
                    username, allCommands.size());
        }

    }
}
