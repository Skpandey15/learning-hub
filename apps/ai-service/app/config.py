from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    environment: str = "development"
    internal_service_token: str = Field(min_length=32)
    litellm_base_url: str = "http://litellm:4000"
    litellm_api_key: str = Field(min_length=8)
    study_model: str = "openai/gpt-5-mini"
    request_timeout_seconds: float = Field(default=60.0, ge=1.0, le=180.0)


@lru_cache
def get_settings() -> Settings:
    return Settings()
