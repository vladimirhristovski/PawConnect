from pydantic_settings import BaseSettings


class Settings(BaseSettings):

    # Ollama config
    ollama_host: str = "http://localhost:11434"
    ollama_model: str = "qwen2.5:1.5b"
    ollama_timeout: float = 15.0
    
    # App config
    app_title: str = "PawConnect Pet Matcher API"
    app_description: str = "Recommends pets based on lifestyle traits"
    debug: bool = False
    log_level: str = "INFO"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


settings = Settings()
