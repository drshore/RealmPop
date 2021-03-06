//
// RealmPop server schema
//

// Player object, contains data about each connected game player
function Player() { }

Player.schema = {
  name: 'Player',
  primaryKey: 'id',
  properties: {
    id: {type: 'string'},
    name: {type: 'string'},
    available: {type: 'bool', default: false},
    challenger: {type: 'Player', optional: true},
    currentGame: {type: 'Game', optional: true}
  }
};

// Game object, containing data about each match
function Game() { }

Game.schema = {
  name: 'Game',
  properties: {
    player1: {type: 'Side', optional: true},
    player2: {type: 'Side', optional: true},
    numbers: {type: 'string'}
  }
}

// Side object, data about each "side" in a multiplayer game
function Side() { }

Side.schema = {
  name: 'Side',
  properties: {
    playerId: {type: 'string'},
    name: {type: 'string'},
    left: {type: 'int'},
    time: {type: 'double'},
    failed: {type: 'bool'}
  }
}

// Score object, used to submit high scores to the server
function Score() { }

Score.schema = {
  name: 'Score',
  properties: {
    name: {type: 'string'},
    time: {type: 'double'},
  }
}

exports.Player = Player;
exports.Game = Game;
exports.Side = Side;
exports.Score = Score;
