CREATE TABLE feed_bookmarks
(
    id           VARCHAR(36)  NOT NULL,
    user_id      VARCHAR(36)  NOT NULL,
    feed_item_id VARCHAR(36)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_feed_bookmarks_user_feed (user_id, feed_item_id)
);
