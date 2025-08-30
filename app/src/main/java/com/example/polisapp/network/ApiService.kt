package com.example.polisapp.network

import com.example.polisapp.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    

    @POST("upsertTeacher")
    suspend fun upsertTeacher(@Body teacherDto: TeacherDto): Response<RespSingleDto<TeacherDto>>

    @POST("filterTeachers")
    suspend fun filterTeachers(@Body filterDto: SimpleStringFilterDto): Response<RespSliceDto<TeacherDto>>

    @POST("deleteTeacher")
    suspend fun deleteTeacher(@Body teacherIdDto: LongIdDto): Response<RespSingleDto<Void>> 

    @POST("getTeacher")
    suspend fun getTeacher(@Body teacherIdDto: LongIdDto): Response<RespSingleDto<TeacherDto>>


    

    @POST("upsertCourse")
    suspend fun upsertCourse(@Body courseDto: CourseDto): Response<RespSingleDto<CourseDto>>

    @POST("filterCourses")
    suspend fun filterCourses(@Body filterDto: SimpleStringFilterDto): Response<RespSliceDto<CourseDto>>

    @POST("deleteCourse")
    suspend fun deleteCourse(@Body courseIdDto: LongIdDto): Response<RespSingleDto<Void>>

    @POST("getCourse")
    suspend fun getCourse(@Body courseIdDto: LongIdDto): Response<RespSingleDto<CourseDto>>

    @POST("associateTeacherToCourse")
    suspend fun associateTeacherToCourse(@Body assocDto: CourseTeacherAssocDto): Response<RespSingleDto<Void>>

    @POST("removeTeacherFromCourse")
    suspend fun removeTeacherFromCourse(@Body assocDto: CourseTeacherAssocDto): Response<RespSingleDto<Void>>


    

    @POST("upsertStudent")
    suspend fun upsertStudent(@Body studentDto: StudentDto): Response<RespSingleDto<StudentDto>>

    @POST("filterStudents")
    suspend fun filterStudents(@Body filterDto: SimpleStringFilterDto): Response<RespSliceDto<StudentDto>>

    @POST("deleteStudent")
    suspend fun deleteStudent(@Body studentIdDto: LongIdDto): Response<RespSingleDto<Void>>

    @POST("associateStudentToCourse")
    suspend fun associateStudentToCourse(@Body assocDto: CourseStudentAssocDto): Response<RespSingleDto<Void>>

    @POST("removeStudentFromCourse")
    suspend fun removeStudentFromCourse(@Body assocDto: CourseStudentAssocDto): Response<RespSingleDto<Void>>

    @POST("getStudent")
    suspend fun getStudent(@Body studentIdDto: LongIdDto): Response<RespSingleDto<StudentDto>>

}