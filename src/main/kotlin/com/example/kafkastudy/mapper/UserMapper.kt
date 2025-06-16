package com.example.kafkastudy.mapper

import com.example.kafkastudy.entity.User
import org.apache.ibatis.annotations.*

@Mapper
interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    fun findById(id: Long): User?
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    fun findByEmail(email: String): User?
    
    @Select("SELECT * FROM users")
    fun findAll(): List<User>
    
    @Insert("""
        INSERT INTO users (email, name, age, created_at) 
        VALUES (#{email}, #{name}, #{age}, #{createdAt})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(user: User): Int
    
    @Update("""
        UPDATE users 
        SET name = #{name}, age = #{age}, updated_at = #{updatedAt} 
        WHERE id = #{id}
    """)
    fun update(user: User): Int
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    fun deleteById(id: Long): Int
} 