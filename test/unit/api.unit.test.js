const supertest = require('supertest');
const app = require('../../src/app');
const AWS = require('aws-sdk');

const request = supertest(app);

const data = {
    Items: [
        {
            id: 0,
            title: 'Title 1',
            year: 2000,
            director: 'Director 1'
        },
        {
            id: 1,
            title: 'Title 2',
            year: 2000,
            director: 'Director 2'
        }
    ]
}

const getMockedMovie = (number) => data.Items[number];

describe('Unit tests', () => {

    beforeAll(() => {
        jest.mock('aws-sdk');
        AWS.DynamoDB.DocumentClient.prototype.get.mockImplementation((_, cb) => {
            cb(null, data);
        });
    });

    afterAll(() => {
        AWS.DynamoDB.DocumentClient.mockReset()
    });


    test('Recuperar todas las películas', async () => {
        const response = await request.get('/api/films').expect(200);
        const [{title: title1}, {title: title2}] = response.body;
        
        expect(title1).toBe(getMockedMovie(0).title);
        expect(title2).toBe(getMockedMovie(1).title);
        expect(response.statusCode).toBe(200);
    });

    test('Añadir una nueva película', async () => {
        const film = { title: 'Title 3', year : 2000, director: 'Director 3'};
        const response = await request.post('/api/films').send(film).expect(201);
        const { title, year, director, id} = response.body;

        expect(id).toBe(0);
        expect(title).toBe(film.title);
        expect(year).toBe(film.year);
        expect(director).toBe(film.director);
        expect(response.statusCode).toBe(201);
    });

    test('Añadir una nueva pelicula con cuerpo vacio', async () => {

        const response = await request.post('/api/films').send(null).expect(400);

        expect(response.statusCode).toBe(400);
        expect(response.error).not.toBeNull();
    });

})