const supertest = require('supertest');
const { GenericContainer } = require("testcontainers");
const app = require('../../src/app');
const AWS = require('aws-sdk');
const createTableIfNotExist = require('../../src/db/createTable');

const request = supertest(app);
let dynamoContainer;
const setData = async (request) => {
    const film = { title: 'Title 1', year : 2000, director: 'Director 1'};
    await request.post('/api/films').send(film).expect(201);
}

describe('Tests container', () => {

    beforeAll(async () => {
        dynamoContainer = await new GenericContainer('amazon/dynamodb-local','1.13.6')
            .withExposedPorts(8000)
            .start()
            .catch((err)=>{console.log(err)});

        const dynamoPort = dynamoContainer.getMappedPort(8000);

        AWS.config.update({
            region: process.env.AWS_REGION || 'local',
            endpoint: process.env.AWS_DYNAMO_ENDPOINT || `http://localhost:8000`,
            accessKeyId: "xxxxxx", // No es necesario poner nada aquí
            secretAccessKey: "xxxxxx" // No es necesario poner nada aquí
        });
        await createTableIfNotExist('films');
        await setData(request);
    });

    afterAll(async () => {
        await dynamoContainer.stop();
    });

    test('Recuperar todas las películas', async () => {
        const response = await request.get('/api/films').timeout(10000).expect(200);
        const [{title, id}] = response.body;

        expect(response.statusCode).toBe(200);
        expect(id).toBe(0);
        expect(title).toBe('Title 1');
    });

    test('Añadir una nueva película', async () => {
        const film = { title: 'Title 2', year : 2000, director: 'Director 2'};
        const response = await request.post('/api/films').send(film).expect(201);
        const { title, year, director, id} = response.body;

        expect(id).toBe(1); // DB preloaded with one film by default
        expect(title).toBe(film.title);
        expect(year).toBe(film.year);
        expect(director).toBe(film.director);
        expect(response.statusCode).toBe(201);
    });
});