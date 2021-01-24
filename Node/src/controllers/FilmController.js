const express = require('express');
const bodyParser = require('body-parser');
const AWS = require('aws-sdk');

const table ="films"

const router = express.Router();

router.use(bodyParser.json());

var index = 0;

router.get('/', function (req, res) {

    // Create client at function level to have right config
    const dynamobDB = new AWS.DynamoDB.DocumentClient();

    var params = {
        TableName : table,
    };
  
    dynamobDB.scan(params, function(err, data) {
        if (err) {
            console.error("Error al recuperar las peliculas: ", JSON.stringify(err, null, 2));
            res.status(400).send(err)
        } else {
            res.send(data.Items);
        }
    });
  
});
  
router.post('/', function (req, res) {

    // Create client at function level to have right config
    const dynamobDB = new AWS.DynamoDB.DocumentClient();

    var film = req.body;
    film.id = index;

    var params = {
        TableName:table,
        Item: film
    };

    dynamobDB.put(params, function(err, data) {
        if (err) {
            console.error("Error al procesar el JSON:", JSON.stringify(err, null, 2));
            res.status(400).send(err);
        } else {
            index++;
            res.status(201).send(film);
        }
    });

});

module.exports = router