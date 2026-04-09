CREATE DATABASE IpManagerDB;
GO

USE IpManagerDB;
GO

CREATE TABLE networks (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    network_ip VARCHAR(15) NOT NULL,
    cidr INT NOT NULL,
    network_address_bigint BIGINT NOT NULL,
    broadcast_address_bigint BIGINT NOT NULL,
    created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
);
GO

CREATE TABLE assigned_ips (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    network_id BIGINT NOT NULL,
    ip_address VARCHAR(15) NOT NULL,
    ip_as_bigint BIGINT NOT NULL,
    assigned_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_assigned_ips_networks
        FOREIGN KEY (network_id) REFERENCES networks(id),
    CONSTRAINT UQ_network_ip UNIQUE (network_id, ip_address)
);
GO