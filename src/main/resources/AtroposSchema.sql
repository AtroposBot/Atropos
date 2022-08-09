-- MySQL dump 10.13  Distrib 8.0.26, for Linux (x86_64)
--
-- Host: localhost    Database: eris
-- ------------------------------------------------------
-- Server version	8.0.26

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permissions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `permission` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `permission` (`permission`)
) ENGINE=InnoDB AUTO_INCREMENT=111 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `punishments`
--

DROP TABLE IF EXISTS `punishments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `punishments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id_punished` int NOT NULL DEFAULT '0',
  `name_punished` varchar(255) DEFAULT NULL,
  `discrim_punished` varchar(255) DEFAULT NULL,
  `user_id_punisher` int NOT NULL DEFAULT '0',
  `name_punisher` varchar(255) DEFAULT NULL,
  `discrim_punisher` varchar(255) DEFAULT NULL,
  `server_id` int NOT NULL DEFAULT '0',
  `punishment_type` varchar(255) NOT NULL,
  `punishment_date` bigint NOT NULL DEFAULT '0',
  `punishment_message` varchar(4000) DEFAULT NULL,
  `permanent` tinyint(1) NOT NULL DEFAULT '1',
  `automatic` tinyint(1) NOT NULL DEFAULT '0',
  `punishment_end_date` bigint DEFAULT NULL,
  `punishment_end_reason` varchar(4000) DEFAULT NULL,
  `did_dm` tinyint(1) NOT NULL DEFAULT '0',
  `end_date_passed` tinyint(1) NOT NULL DEFAULT '0',
  `punishment_ender` int DEFAULT NULL,
  `name_punishment_ender` varchar(255) DEFAULT NULL,
  `discrim_punishment_ender` varchar(255) DEFAULT NULL,
  `automatic_end` tinyint(1) NOT NULL DEFAULT '0',
  `batch_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `user_id` (`user_id_punished`),
  KEY `server_id` (`server_id`),
  KEY `punishments_users_id_fk` (`user_id_punisher`),
  CONSTRAINT `punishments_ibfk_1` FOREIGN KEY (`user_id_punished`) REFERENCES `users` (`id`) ON UPDATE CASCADE,
  CONSTRAINT `punishments_ibfk_2` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `punishments_users_id_fk` FOREIGN KEY (`user_id_punisher`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=621 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_blacklist`
--

DROP TABLE IF EXISTS `server_blacklist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `server_blacklist` (
  `id` int NOT NULL AUTO_INCREMENT,
  `server_id` int NOT NULL,
  `regex_trigger` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `server_id` (`server_id`),
  CONSTRAINT `server_blacklist_ibfk_1` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_command_uses`
--

DROP TABLE IF EXISTS `server_command_uses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `server_command_uses` (
  `id` int NOT NULL AUTO_INCREMENT,
  `server_id` int NOT NULL,
  `command_user_id` int NOT NULL,
  `command_contents` varchar(4000) NOT NULL,
  `date` bigint DEFAULT NULL,
  `success` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `server_command_uses_id_uindex` (`id`),
  KEY `server_command_uses_servers_id_fk` (`server_id`),
  KEY `server_command_uses_users_id_fk` (`command_user_id`),
  CONSTRAINT `server_command_uses_servers_id_fk` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `server_command_uses_users_id_fk` FOREIGN KEY (`command_user_id`) REFERENCES `users` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1177 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_messages`
--

DROP TABLE IF EXISTS `server_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `server_messages` (
  `id` int NOT NULL AUTO_INCREMENT,
  `message_id_snowflake` bigint NOT NULL,
  `server_id` int NOT NULL DEFAULT '0',
  `server_id_snowflake` bigint NOT NULL DEFAULT '0',
  `user_id` int NOT NULL DEFAULT '0',
  `user_id_snowflake` bigint NOT NULL DEFAULT '0',
  `date` bigint NOT NULL DEFAULT '0',
  `content` varchar(4000) DEFAULT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `message_data` varchar(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `server_messages_id_uindex` (`id`),
  KEY `server_messages_servers_id_fk` (`server_id`),
  KEY `server_messages_users_id_fk` (`user_id`),
  CONSTRAINT `server_messages_servers_id_fk` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `server_messages_users_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=73217 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_properties`
--

DROP TABLE IF EXISTS `server_properties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `server_properties` (
  `id` int NOT NULL AUTO_INCREMENT,
  `server_id` int NOT NULL,
  `server_id_snowflake` bigint NOT NULL DEFAULT '0',
  `server_name` varchar(255) DEFAULT 'default_server_name',
  `member_count_on_bot_join` bigint NOT NULL DEFAULT '0',
  `muted_role_id_snowflake` bigint DEFAULT NULL,
  `member_log_channel_snowflake` bigint DEFAULT NULL,
  `message_log_channel_snowflake` bigint DEFAULT NULL,
  `guild_log_channel_snowflake` bigint DEFAULT NULL,
  `punishment_log_channel_snowflake` bigint DEFAULT NULL,
  `stop_joins` tinyint(1) NOT NULL DEFAULT '0',
  `modmail_channel_snowflake` bigint DEFAULT NULL,
  `messages_to_warn` int NOT NULL DEFAULT '7',
  `warns_to_mute` int NOT NULL DEFAULT '3',
  `pings_to_warn` int NOT NULL DEFAULT '5',
  `joins_to_antiraid` int NOT NULL DEFAULT '0',
  `anti_scam` tinyint(1) NOT NULL DEFAULT '1',
  `dehoist` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `server_properties_servers_id_fk` (`server_id`),
  KEY `server_properties_servers_server_id_fk` (`server_id_snowflake`),
  CONSTRAINT `server_properties_servers_id_fk` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_role_permissions`
--

DROP TABLE IF EXISTS `server_role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `server_role_permissions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `server_id` int NOT NULL,
  `permission_id` int NOT NULL,
  `role_id_snowflake` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `server_id` (`server_id`),
  KEY `permission_id` (`permission_id`),
  CONSTRAINT `server_role_permissions_ibfk_1` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `server_role_permissions_ibfk_2` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=94 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `server_user`
--

DROP TABLE IF EXISTS `server_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `server_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `server_id` int NOT NULL,
  `date` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `server_user_ibfk_1` (`user_id`),
  KEY `server_user_ibfk_2` (`server_id`),
  CONSTRAINT `server_user_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `server_user_ibfk_2` FOREIGN KEY (`server_id`) REFERENCES `servers` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7317 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `servers`
--

DROP TABLE IF EXISTS `servers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `servers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `date` bigint NOT NULL DEFAULT '0',
  `server_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `servers_server_id_uindex` (`server_id`)
) ENGINE=InnoDB AUTO_INCREMENT=48 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id_snowflake` bigint NOT NULL DEFAULT '0',
  `date` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  UNIQUE KEY `user_id_UNIQUE` (`user_id_snowflake`)
) ENGINE=InnoDB AUTO_INCREMENT=7060 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ENCRYPTION='Y';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-08-08 23:44:07
