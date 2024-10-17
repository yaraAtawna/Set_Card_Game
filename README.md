# Set Card Game

# General Description
This project implements a simplified version of the Set card game in a Java environment. The game utilizes concurrent programming principles and focuses on unit testing, offering a practical experience in Java Threads and Synchronization.

# Game Overview
The game consists of a deck of 81 cards, each featuring four characteristics: color, number, shape, and shading. The goal is for players to identify a "legal set" of three cards based on specific rules:

For each feature (color, number, shape, shading), the three cards must either:
All be the same, or
All be different.

# Features

Two Player Types: Supports both human players (using keyboard input) and non-human players (simulated by threads generating random key presses).

Dynamic Gameplay: The game reshuffles and redraws cards every minute if no legal sets are available.

Scoring System: Players earn points for correctly identifying legal sets, with penalties for incorrect submissions.

# Game Rules
The game starts with 12 drawn cards placed on a 3x4 grid.
Players place tokens on the cards to indicate their selections.
Players must ask the dealer to verify if the selected cards form a legal set.
If the set is legal, the cards are replaced, and the player earns a point.
If the set is not legal, the player receives a penalty, freezing their ability to act for a specified period.
