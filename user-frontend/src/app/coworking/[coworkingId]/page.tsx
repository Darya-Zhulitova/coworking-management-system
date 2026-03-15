type CoworkingPageProps = {
    params: Promise<{
        coworkingId: string;
    }>;
};

export default async function CoworkingPage({
                                                params,
                                            }: CoworkingPageProps) {
    const { coworkingId } = await params;

    return (
        <section>
            <h1>Coworking page</h1>
            <p>Coworking: {coworkingId}</p>
        </section>
    );
}