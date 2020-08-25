defmodule HikingmapWeb.Router do
  use HikingmapWeb, :router
  use Pow.Phoenix.Router

  pipeline :browser do
    plug :accepts, ["html"]
    plug :fetch_session
    plug :fetch_live_flash
    plug :put_root_layout, {HikingmapWeb.LayoutView, :root}
    plug :protect_from_forgery
    plug :put_secure_browser_headers
  end

  pipeline :protected do
    plug Pow.Plug.RequireAuthenticated,
      error_handler: Pow.Phoenix.PlugErrorHandler
  end

  pipeline :api do
    plug :accepts, ["json"]
    plug HikingmapWeb.APIAuth.Plug, otp_app: :hikingmap
  end

  pipeline :api_protected do
    plug Pow.Plug.RequireAuthenticated, error_handler: HikingmapWeb.APIAuth.ErrorHandler
  end

  scope "/" do
    pipe_through :browser
    pow_routes()
  end

  scope "/", HikingmapWeb do
    pipe_through :browser

    live "/", PageLive, :index
  end

  scope "/", HikingmapWeb do
    pipe_through [:browser, :protected]

    # Add your protected routes here
  end

  scope "/api/v1", HikingmapWeb.API.V1, as: :api_v1 do
    pipe_through [:api, :api_protected]

    # Your protected API endpoints here
  end

  scope "/api/v1", HikingmapWeb.API.V1, as: :v1_api do
    pipe_through :api

    resources "/registration", RegistrationController, singleton: true, only: [:create]
    resources "/session", SessionController, singleton: true, only: [:create, :delete]
    post "/session/renew", SessionController, :renew
  end

  # Enables LiveDashboard only for development
  #
  # If you want to use the LiveDashboard in production, you should put
  # it behind authentication and allow only admins to access it.
  # If your application does not have an admins-only section yet,
  # you can use Plug.BasicAuth to set up some basic authentication
  # as long as you are also using SSL (which you should anyway).
  if Mix.env() in [:dev, :test] do
    import Phoenix.LiveDashboard.Router

    scope "/" do
      pipe_through :browser
      live_dashboard "/dashboard", metrics: HikingmapWeb.Telemetry
    end
  end
end
