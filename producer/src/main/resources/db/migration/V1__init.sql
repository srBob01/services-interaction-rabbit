-- 1) Создаём таблицу владельцев
CREATE TABLE owners
(
    email        VARCHAR(100) PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    phone_number VARCHAR(100) NOT NULL
);

-- 2) Создаём таблицу товаров (Items)
CREATE TABLE items
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    owner_email VARCHAR(100) NOT NULL,
    CONSTRAINT fk_item_owner_email FOREIGN KEY (owner_email) REFERENCES owners (email)
);

-- 3) Создаём таблицу карточек (Cards)
CREATE TABLE cards
(
    id          SERIAL PRIMARY KEY,
    card_number VARCHAR(50)  NOT NULL,
    owner_email VARCHAR(100) NOT NULL,
    CONSTRAINT fk_card_owner_email FOREIGN KEY (owner_email) REFERENCES owners (email)
);

-- Добавляем 20 владельцев
INSERT INTO owners (email, name, phone_number)
VALUES ('owner1@example.com', 'Owner 1', '+79999991131'),
       ('owner2@example.com', 'Owner 2', '+79999991132'),
       ('owner3@example.com', 'Owner 3', '+79999991133'),
       ('owner4@example.com', 'Owner 4', '+79999991134'),
       ('owner5@example.com', 'Owner 5', '+79999991135'),
       ('owner6@example.com', 'Owner 6', '+79999991136'),
       ('owner7@example.com', 'Owner 7', '+79999991137'),
       ('owner8@example.com', 'Owner 8', '+79999991138'),
       ('owner9@example.com', 'Owner 9', '+79999991139'),
       ('owner10@example.com', 'Owner 10', '+79999991110'),
       ('owner11@example.com', 'Owner 11', '+79999991111'),
       ('owner12@example.com', 'Owner 12', '+79999991112'),
       ('owner13@example.com', 'Owner 13', '+79999991113'),
       ('owner14@example.com', 'Owner 14', '+79999991114'),
       ('owner15@example.com', 'Owner 15', '+79999991115'),
       ('owner16@example.com', 'Owner 16', '+79999991116'),
       ('owner17@example.com', 'Owner 17', '+79999991117'),
       ('owner18@example.com', 'Owner 18', '+79999991118'),
       ('owner19@example.com', 'Owner 19', '+79999991119'),
       ('owner20@example.com', 'Owner 20', '+79999991120');

-- Добавляем по 2 товара для каждого владельца
INSERT INTO items (name, owner_email)
SELECT CONCAT('ItemA_of_', o.email), o.email
FROM owners o
UNION ALL
SELECT CONCAT('ItemB_of_', o.email), o.email
FROM owners o;

-- Добавим по 1 карточке на владельца
INSERT INTO cards (card_number, owner_email)
SELECT CONCAT('CARD-', o.email), o.email
FROM owners o;
