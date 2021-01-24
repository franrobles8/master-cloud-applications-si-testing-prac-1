const express = require('express');
const app = express();

var FilmController = require('./controllers/FilmController');
app.use('/api/films', FilmController);
module.exports = app;