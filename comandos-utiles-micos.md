
docker-compose up -d

docker-compose ps

docker-compose logs -f

cd client-app
mvn clean javafx:run

username: admin
password: admin123


docker exec -it messaging_db psql -U messaging_user -d messaging_app


SELECT * FROM users;

SELECT id, username, email, status, max_connections, max_files_per_day, created_at 
FROM users;

SELECT * FROM users WHERE status = 'APPROVED';

SELECT m.id, u1.username as sender, u2.username as receiver, 
       m.message_type, m.content, m.file_name, m.sent_at
FROM messages m
JOIN users u1 ON m.sender_id = u1.id
JOIN users u2 ON m.receiver_id = u2.id
ORDER BY m.sent_at DESC;

docker exec -it messaging_db psql -U messaging_user -d messaging_app -c "SELECT * FROM users;"

docker exec -it messaging_db psql -U messaging_user -d messaging_app -c "SELECT * FROM messages ORDER BY sent_at DESC LIMIT 10;"

docker-compose restart

docker-compose restart server-app

docker-compose restart web-app

docker-compose down

docker-compose down -v
docker logs -f messaging_server
docker logs -f messaging_web
docker logs -f messaging_db
docker exec -it messaging_server sh
docker exec -it messaging_web sh
