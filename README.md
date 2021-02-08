<p align="center">
  <img width="500" height="500" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/app/src/main/res/drawable/logo.png">
</p>

## Intro
Cards Against Humanity is a party game in which players complete fill-in-the-blank statements using words or phrases typically deemed as offensive, risqué or politically incorrect printed on playing cards. 

## Rules
This game to work propely require at least three people, at least two contenders and only one judge.<br>
On start, each player receive ten white cards.<br>

Player that create match begins as the "Card Czar" and plays a black card, face up.
The other players choose enough cards to fill gaps in black card, trying to form a complete sentence.

The Card Czar receive player choices without knowing who made each choice, then picks the funniest play and whoever submitted it gets one point.

After the round, a new player becomes the Card Czar, and everyone receive enough cards to have ten in their deck.

The part of speech of a white card is a noun or gerund, including both single words and phrase constructions. Black cards are either fill-in-the-blank statements or questions. Both white and black cards break these rules on rare occasions.

The game ends with one or more winners after certain amount of rounds. A point is given to a winner or splitted equally in case of multiple winners.

## App activities

### Sign-In Activity
CAH app start with sign-in request, where user can login/register himself using email and password or using Google sign-in.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/SignIn.png">
</p>

###  Choose Nickname Activity
When user has logged in, he need to choose a nickname.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/ChooseNickname.png">
</p>

### Choose Match Activity
After choosing a nickname, user can join a match or create a new one setting match name, optinal password and match rounds.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/ChooseMatch.png">
</p>

### Wait for players activity
If user join a match he need to wait that match dealer start the match or going back to choose match activity.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/WaitPlayer.png">
</p>
If user is match dealer when at least two other users take part to the game he can start the match or he can delete actual match and return to choose match activity.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/WaitCreator.png">
</p>

### Shuffling and Distributing
After match start cards are shuffled and distribuited to players.

### Game Activity
In this activity contenders can choose white card(s) to fill black card gaps(s) and send it(them) to card Czar.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/ContenderChoice.png">
</p>

Card Czar will see anonymous contenders choices once they have done and choose his favorite one giving a point to the relative player.
<p align="center">
<img width="180" height="320" style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/CzarChoice.png">
</p>

### Awarding Activity
In this activity player are shown whether they have win or lost.
If game is finished, match will be removed from DB, user points will be updated and players come back to Choose Nickname activity.

## Life Cycle diagram
For better understanding i've drawn a diagram.
<p align="center">
<img style="float: right;" src="https://github.com/Ghinne/Cards-Against-Humanity/blob/master/Screenshots/LifeCycle.png">
</p>
As you can see many activities can return to the Choose Nickname activity, this is due to the fact that in case of a failure of an activity or a communication with the database, the game is interrupted and the players return to that activity.
To make the app more orderly and less repetitive I used the DBCommunicator class which, once extended for the different activities, allows communication with the database.
As for the database, I opted to use Cloud Firestore, for the ease of use and the ability to perform more complex queries.

## Game Languages
Game and cards supported languages.
<ul>
  <li>English</li>
  <li>Italian</li>
</ul>
