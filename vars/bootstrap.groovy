def call(Map args = [:]) {
    def repo_url = args.repo_url ?: null
    def branch = args.branch ?: null
    def flow = args.flow ?: null
    def environments = args.environments ?: null

    node("master") {
        stage ("Checkout SCM") {

        }
        stage ("Setting Envs") {

        }
        stage ("Maven Build") {

        }
        stage ("Unit Testing") {

        }
        stage ("Code Quality") {

        }
        stage ("Integration Testing") {

        }
        stage ("Acceptance Testing") {

        }
        stage ("Upload to Artifactory") {

        }
    }
}