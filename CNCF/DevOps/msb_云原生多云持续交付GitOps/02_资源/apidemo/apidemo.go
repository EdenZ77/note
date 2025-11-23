package main

import (
	"github.com/gin-gonic/gin"
	"net/http"
)

func main() {
	r := gin.Default()
	r.GET("/info", Info)
	r.Run(":10088")
}

func Info(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{
		"version": "1.1.1",
	})
}
