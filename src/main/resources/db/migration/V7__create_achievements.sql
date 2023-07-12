CREATE TABLE "achievements" (
    achievement_id SERIAL PRIMARY KEY,
    name TEXT,
    description TEXT
);

CREATE TABLE "user_achievements" (
    user_id BIGSERIAL REFERENCES users(id),
    achievement_id integer REFERENCES achievements(achievement_id),
    PRIMARY KEY (user_id, achievement_id)
)