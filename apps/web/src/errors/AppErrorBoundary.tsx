import { Component, type ReactNode } from "react";

interface Props {
  children: ReactNode;
}

interface State {
  failed: boolean;
  incidentId: string;
}

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { failed: false, incidentId: "" };

  static getDerivedStateFromError(): State {
    return { failed: true, incidentId: crypto.randomUUID() };
  }

  componentDidCatch(): void {
    // Do not emit exception messages, component props, tokens, or browser state.
    // A future telemetry adapter can submit the opaque incident ID with user consent.
    console.error("ui_render_error", { incidentId: this.state.incidentId });
  }

  private reload = (): void => {
    globalThis.location.reload();
  };

  render(): ReactNode {
    if (!this.state.failed) {
      return this.props.children;
    }

    return (
      <main className="page-shell" role="main">
        <p className="eyebrow">Something went wrong</p>
        <h1>We couldn’t display this page.</h1>
        <p className="lede">Your data has not been changed. Reload the application to try again.</p>
        <button type="button" className="primary-action" onClick={this.reload}>
          Reload application
        </button>
        <p className="incident-id">Reference: {this.state.incidentId}</p>
      </main>
    );
  }
}
